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

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

class AcceleratorDriverTest {

	/** an accelerator which searches until it is interrupted, counting the searches which run at the same time */
	private static class BlockingAccelerator implements BruteForceAccelerator {

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

	@Test
	void concurrentRacesReachTheAcceleratorOneAtATime() throws Exception {
		BlockingAccelerator accelerator = new BlockingAccelerator();
		AcceleratorDriver<String> driver = new AcceleratorDriver<>(accelerator, AcceleratorMode.AUTO, 0L);
		int races = 8;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(races);
		List<Throwable> failures = new ArrayList<>();
		for (int i = 0; i < races; i++) {
			Thread thread = new Thread(() -> {
				try {
					start.await();
					// the own search finishes after a while, so the accelerator's thread has time to join
					String result = driver.search(null, 0L, () -> false, interrupt -> {
						LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
						return "own";
					}, winner -> "accelerated");
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
}
