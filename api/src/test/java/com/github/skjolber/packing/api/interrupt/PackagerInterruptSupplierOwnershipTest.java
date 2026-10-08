package com.github.skjolber.packing.api.interrupt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class PackagerInterruptSupplierOwnershipTest {

	private static class CountingInterruptSupplier implements PackagerInterruptSupplier {

		private final AtomicInteger closed = new AtomicInteger();

		@Override
		public boolean getAsBoolean() {
			return false;
		}

		@Override
		public void close() {
			closed.incrementAndGet();
		}
	}

	@Test
	void builderReturnsUserInterruptUnchangedWithoutDeadline() {
		CountingInterruptSupplier interrupt = new CountingInterruptSupplier();

		assertThat(PackagerInterruptSupplierBuilder.builder().withInterrupt(interrupt).build()).isSameAs(interrupt);
	}

	@Test
	void closingDeadlineSupplierWithDelegateCancelsTheDeadlineAndLeavesTheDelegateOpen() {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setRemoveOnCancelPolicy(true);
		try {
			CountingInterruptSupplier interrupt = new CountingInterruptSupplier();

			PackagerInterruptSupplier supplier = PackagerInterruptSupplierBuilder.builder()
					.withScheduledThreadPoolExecutor(scheduler)
					.withDeadline(System.currentTimeMillis() + 600_000L)
					.withInterrupt(interrupt)
					.build();
			assertThat(supplier).isInstanceOf(DelegateDeadlineCheckPackagerInterruptSupplier.class);
			assertThat(scheduler.getQueue()).hasSize(1);

			supplier.close();

			assertThat(scheduler.getQueue()).isEmpty();
			assertThat(interrupt.closed.get()).isZero();
		} finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	void closingDeadlineSuppliersWithoutFutureDoesNotFail() {
		new DeadlineCheckPackagerInterruptSupplier().close();
		new DelegateDeadlineCheckPackagerInterruptSupplier(new CountingInterruptSupplier()).close();
	}
}
