package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.accelerator.FlatTestInstances.Instance;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

class AcceleratorDriverTest {

	/** an accelerator which searches until it is interrupted, or wins at once, counting the searches which run at the same time */
	private static class StubAccelerator implements BruteForceAccelerator {

		volatile boolean wins;
		final AtomicInteger running = new AtomicInteger();
		final AtomicInteger mostAtOnce = new AtomicInteger();
		final AtomicInteger entered = new AtomicInteger();

		@Override
		public boolean supports(FlatProblem problem) {
			return true;
		}

		@Override
		public Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
			entered.incrementAndGet();
			int now = running.incrementAndGet();
			mostAtOnce.accumulateAndGet(now, Math::max);
			try {
				if(wins) {
					return Optional.of(new AcceleratorWinner(new int[problem.n()], new int[problem.n()], problem.n(), 1L, 1L, 1L, true));
				}
				while (!interrupt.getAsBoolean()) {
					LockSupport.parkNanos(100_000L);
				}
				throw new PackagerInterruptedException();
			} finally {
				running.decrementAndGet();
			}
		}

		@Override
		public void close() {
		}
	}

	private static FlatProblem problem(int boxes) {
		Instance instance = FlatTestInstances.random(1, boxes, boxes);
		Container container = instance.container;
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
	}

	/** an own search which takes a while, unless it is interrupted (the accelerator won) */
	private static AcceleratorDriver.Search<String> ownSearch(long millis) {
		return interrupt -> {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
			while (System.nanoTime() < deadline) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				LockSupport.parkNanos(100_000L);
			}
			return "own";
		};
	}

	@Test
	void concurrentRacesReachTheAcceleratorOneAtATime() throws Exception {
		StubAccelerator accelerator = new StubAccelerator();
		AcceleratorDriver<String> driver = new AcceleratorDriver<>(accelerator, AcceleratorMode.AUTO, new FixedAcceleratorGate(0L));
		FlatProblem problem = problem(6);
		int races = 8;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(races);
		List<Throwable> failures = new ArrayList<>();
		for (int i = 0; i < races; i++) {
			Thread thread = new Thread(() -> {
				try {
					start.await();
					// the own search finishes after a while, so the accelerator's thread has time to join
					String result = driver.search(problem, 0L, () -> false, ownSearch(20), winner -> "accelerated");
					assertThat(result).isEqualTo("own");
				} catch (Throwable e) {
					synchronized (failures) {
						failures.add(e);
					}
				} finally {
					done.countDown();
				}
			});
			thread.setDaemon(true);
			thread.start();
		}
		start.countDown();
		assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(failures).isEmpty();
		assertThat(driver.raceCpuCount.get()).isEqualTo(races);
		assertThat(driver.acceleratedCount.get()).isZero();
		// the searches which lost their race before their turn were dropped from the queue, and the others ran one at a time
		assertThat(accelerator.mostAtOnce.get()).isLessThanOrEqualTo(1);
		assertThat(accelerator.entered.get()).isLessThan(races);
		driver.close();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (accelerator.running.get() > 0 && System.nanoTime() < deadline) {
			LockSupport.parkNanos(1_000_000L);
		}
		assertThat(accelerator.running.get()).isZero();
	}

	/** The gate of a box count doubles while the accelerator joins and loses, until it no longer joins; it halves back to the floor while the accelerator wins. */
	@Test
	void theGateOfABoxCountAdaptsToTheRaces() throws Exception {
		StubAccelerator accelerator = new StubAccelerator();
		long floor = TimeUnit.MILLISECONDS.toNanos(1);
		AdaptiveAcceleratorGate gate = new AdaptiveAcceleratorGate(floor);
		AcceleratorDriver<String> driver = new AcceleratorDriver<>(accelerator, AcceleratorMode.AUTO, gate);
		FlatProblem six = problem(6);
		FlatProblem seven = problem(7);
		try {
			assertThat(gate.gateNanos(6)).isEqualTo(floor);
			// the own search takes 40 ms: the accelerator joins and loses while the gate is below that, 1, 2, 4, 8, 16, 32 ms, then stays out at 64 ms
			for (int race = 0; race < 8; race++) {
				assertThat(driver.search(six, 0L, () -> false, ownSearch(40), winner -> "accelerated")).isEqualTo("own");
			}
			assertThat(gate.gateNanos(6)).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(32)).isLessThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(256));
			assertThat(accelerator.entered.get()).isGreaterThanOrEqualTo(5).isLessThan(8);
			// another box count has its own gate
			assertThat(gate.gateNanos(7)).isEqualTo(floor);
			assertThat(driver.search(seven, 0L, () -> false, ownSearch(10), winner -> "accelerated")).isEqualTo("own");
			assertThat(gate.gateNanos(7)).isEqualTo(2 * floor);
			// the accelerator wins at once after the gate: the gate of six boxes halves back to the floor
			accelerator.wins = true;
			for (int race = 0; race < 10; race++) {
				assertThat(driver.search(six, 0L, () -> false, ownSearch(1000), winner -> "accelerated")).isEqualTo("accelerated");
			}
			assertThat(gate.gateNanos(6)).isEqualTo(floor);
			assertThat(driver.acceleratedCount.get()).isEqualTo(10);
			assertThat(gate.gateNanos(7)).isEqualTo(2 * floor);
		} finally {
			driver.close();
		}
	}

	@Test
	void theAdaptiveGateStaysBetweenTheFloorAndTheCap() {
		FlatProblem five = problem(5);
		long floor = TimeUnit.MILLISECONDS.toNanos(25);
		AdaptiveAcceleratorGate gate = new AdaptiveAcceleratorGate(floor);
		gate.raced(five, AcceleratorGate.Outcome.ACCELERATOR, 0L);
		assertThat(gate.gateNanos(five)).isEqualTo(floor);
		gate.raced(five, AcceleratorGate.Outcome.PACKAGER_ALONE, 0L);
		assertThat(gate.gateNanos(five)).isEqualTo(floor);
		gate.raced(five, AcceleratorGate.Outcome.PACKAGER, 0L);
		assertThat(gate.gateNanos(five)).isEqualTo(2 * floor);
		for (int i = 0; i < 20; i++) {
			gate.raced(five, AcceleratorGate.Outcome.PACKAGER, 0L);
		}
		assertThat(gate.gateNanos(five)).isEqualTo(AdaptiveAcceleratorGate.CAP_FACTOR * floor);
		for (int i = 0; i < 20; i++) {
			gate.raced(five, AcceleratorGate.Outcome.ACCELERATOR, 0L);
		}
		assertThat(gate.gateNanos(five)).isEqualTo(floor);

		// a floor of zero grows from a millisecond, and returns to zero
		AdaptiveAcceleratorGate ungated = new AdaptiveAcceleratorGate(0L);
		ungated.raced(five, AcceleratorGate.Outcome.PACKAGER, 0L);
		assertThat(ungated.gateNanos(5)).isEqualTo(TimeUnit.MILLISECONDS.toNanos(1));
		ungated.raced(five, AcceleratorGate.Outcome.ACCELERATOR, 0L);
		assertThat(ungated.gateNanos(5)).isZero();
	}

	/** A gate of your own is asked before each race, and told how the race ended and how long it took. */
	@Test
	void aGateOfYourOwnSeesEveryRace() throws Exception {
		StubAccelerator accelerator = new StubAccelerator();
		List<AcceleratorGate.Outcome> outcomes = new ArrayList<>();
		List<Long> times = new ArrayList<>();
		AtomicInteger asked = new AtomicInteger();
		AcceleratorGate gate = new AcceleratorGate() {
			volatile long nanos;

			@Override
			public long gateNanos(FlatProblem problem) {
				asked.incrementAndGet();
				return nanos;
			}

			@Override
			public void raced(FlatProblem problem, Outcome outcome, long nanos) {
				outcomes.add(outcome);
				times.add(nanos);
				// after a lost race, the gate is longer than any own search here
				this.nanos = outcome == Outcome.PACKAGER ? TimeUnit.SECONDS.toNanos(1) : 0L;
			}
		};
		AcceleratorDriver<String> driver = new AcceleratorDriver<>(accelerator, AcceleratorMode.AUTO, gate);
		FlatProblem problem = problem(6);
		try {
			assertThat(driver.search(problem, 0L, () -> false, ownSearch(20), winner -> "accelerated")).isEqualTo("own");
			assertThat(driver.search(problem, 0L, () -> false, ownSearch(20), winner -> "accelerated")).isEqualTo("own");
			assertThat(outcomes).containsExactly(AcceleratorGate.Outcome.PACKAGER, AcceleratorGate.Outcome.PACKAGER_ALONE);
			assertThat(asked.get()).isEqualTo(2);
			assertThat(times.get(0)).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(20));
			accelerator.wins = true;
			assertThat(driver.search(problem, 0L, () -> false, ownSearch(1000), winner -> "accelerated")).isEqualTo("accelerated");
			assertThat(outcomes).hasSize(3);
			assertThat(outcomes.get(2)).isEqualTo(AcceleratorGate.Outcome.ACCELERATOR);
			assertThat(times.get(2)).isLessThan(TimeUnit.MILLISECONDS.toNanos(1000));
		} finally {
			driver.close();
		}
	}
}
