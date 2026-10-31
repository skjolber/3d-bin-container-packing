package com.github.skjolber.packing.accelerator;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

/**
 * Runs a {@link BruteForceAccelerator} for a packager in an {@link AcceleratorMode}: in {@link AcceleratorMode#GPU_ONLY} the accelerator searches
 * and the packager confirms, in {@link AcceleratorMode#AUTO} the packager's own search starts at once and the accelerator joins after a gate on its
 * own thread, the first result winning and stopping the other through the interrupt. The packager supplies its own search and the confirmation of
 * a winner; the driver keeps the counters.
 *
 * @param <R> the result of a container's search
 */
public final class AcceleratorDriver<R> implements AutoCloseable {

	/** the packager's own search of a container, with an interrupt which also stops it when the accelerator has a result */
	public interface Search<R> {
		R run(PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;
	}

	/** the placement of the accelerator's winner by the packager, and its confirmation: null when the winner is not confirmed */
	public interface Confirm<R> {
		R confirm(AcceleratorWinner winner) throws PackagerInterruptedException;
	}

	private final BruteForceAccelerator accelerator;
	private final AcceleratorMode mode;
	private final long gateNanos;
	private ExecutorService executor;

	/** containers searched by the accelerator, with a result which the replay confirmed */
	final AtomicLong acceleratedCount = new AtomicLong();
	/** containers where the accelerator gave up, so the packager searched itself */
	final AtomicLong fallbackCount = new AtomicLong();
	/** containers where the winner of the accelerator was not confirmed by the replay: the packager searched itself; this is a bug */
	final AtomicLong mismatchCount = new AtomicLong();
	/** containers which the packager finished itself while the accelerator searched alongside, or before it joined */
	final AtomicLong raceCpuCount = new AtomicLong();

	public AcceleratorDriver(BruteForceAccelerator accelerator, AcceleratorMode mode, long gateNanos) {
		this.accelerator = accelerator;
		this.mode = mode;
		this.gateNanos = gateNanos;
	}

	public BruteForceAccelerator getAccelerator() {
		return accelerator;
	}

	public AcceleratorMode getMode() {
		return mode;
	}

	/** @return true if the accelerator is used at all: there is one, and the mode is not {@link AcceleratorMode#CPU_ONLY} */
	public boolean isActive() {
		return accelerator != null && mode != AcceleratorMode.CPU_ONLY;
	}

	/**
	 * Search a container the accelerator applies to.
	 *
	 * @param problem the container's problem, which the accelerator supports
	 * @param minLoadVolume a load volume which an earlier container reached, or 0
	 * @param own the packager's own search
	 * @param confirm the confirmation of a winner
	 * @return the result, or null if the accelerator gave up or its winner was not confirmed in {@link AcceleratorMode#GPU_ONLY}: then the packager
	 *         searches itself
	 */
	public R search(FlatProblem problem, long minLoadVolume, PackagerInterruptSupplier interrupt, Search<R> own, Confirm<R> confirm) throws PackagerInterruptedException {
		if(mode == AcceleratorMode.GPU_ONLY) {
			Optional<AcceleratorWinner> found = accelerator.search(problem, minLoadVolume, interrupt);
			if(found.isEmpty()) {
				fallbackCount.incrementAndGet();
				return null;
			}
			return confirmed(found.get(), confirm);
		}
		return race(problem, minLoadVolume, interrupt, own, confirm);
	}

	private R confirmed(AcceleratorWinner winner, Confirm<R> confirm) throws PackagerInterruptedException {
		R result = confirm.confirm(winner);
		if(result == null) {
			mismatchCount.incrementAndGet();
		} else {
			acceleratedCount.incrementAndGet();
		}
		return result;
	}

	private R race(FlatProblem problem, long minLoadVolume, PackagerInterruptSupplier interrupt, Search<R> own, Confirm<R> confirm) throws PackagerInterruptedException {
		final AtomicBoolean cpuDone = new AtomicBoolean();
		final AtomicBoolean acceleratorDone = new AtomicBoolean();
		final PackagerInterruptSupplier acceleratorInterrupt = () -> cpuDone.get() || interrupt.getAsBoolean();
		Future<Optional<AcceleratorWinner>> future = executor().submit(() -> {
			// the gate: the packager alone, unless it is already done
			long deadline = System.nanoTime() + gateNanos;
			while (System.nanoTime() < deadline) {
				if(cpuDone.get()) {
					return Optional.<AcceleratorWinner>empty();
				}
				LockSupport.parkNanos(200_000L);
			}
			if(cpuDone.get()) {
				return Optional.<AcceleratorWinner>empty();
			}
			Optional<AcceleratorWinner> found = accelerator.search(problem, minLoadVolume, acceleratorInterrupt);
			if(found.isPresent() && !cpuDone.get()) {
				// a result: the packager's own search stops
				acceleratorDone.set(true);
			}
			return found;
		});
		final PackagerInterruptSupplier cpuInterrupt = () -> acceleratorDone.get() || interrupt.getAsBoolean();
		try {
			R result = own.run(cpuInterrupt);
			cpuDone.set(true);
			raceCpuCount.incrementAndGet();
			return result;
		} catch (PackagerInterruptedException e) {
			cpuDone.set(true);
			if(interrupt.getAsBoolean()) {
				throw e;
			}
			// the accelerator has a result
			Optional<AcceleratorWinner> found;
			try {
				found = future.get();
			} catch (InterruptedException | ExecutionException ex) {
				found = Optional.empty();
			}
			if(found.isPresent()) {
				R result = confirmed(found.get(), confirm);
				if(result != null) {
					return result;
				}
			} else {
				fallbackCount.incrementAndGet();
			}
			// not confirmed, or gone: the packager searches alone, from the start
			return own.run(interrupt);
		} finally {
			cpuDone.set(true);
			future.cancel(false);
		}
	}

	private synchronized ExecutorService executor() {
		if(executor == null) {
			executor = Executors.newCachedThreadPool(runnable -> {
				Thread thread = new Thread(runnable, "packing-accelerator");
				thread.setDaemon(true);
				return thread;
			});
		}
		return executor;
	}

	@Override
	public synchronized void close() {
		if(executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}
}
