package com.github.skjolber.packing.api.interrupt;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PackagerInterruptSupplierBuilder {

	public static final NegativePackagerInterruptSupplier NEGATIVE = new NegativePackagerInterruptSupplier();
	public static final PositivePackagerInterruptSupplier POSITIVE = new PositivePackagerInterruptSupplier();

	private long deadline = Long.MAX_VALUE;
	private PackagerInterruptSupplier interrupt;
	private ScheduledExecutorService scheduledExecutorService;

	public static PackagerInterruptSupplierBuilder newBuilder() {
		return new PackagerInterruptSupplierBuilder();
	}

	public PackagerInterruptSupplierBuilder withDeadline(long deadline) {
		this.deadline = deadline;
		return this;
	}

	public PackagerInterruptSupplierBuilder withInterrupt(PackagerInterruptSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}
	
	public PackagerInterruptSupplierBuilder withScheduledExecutorService(ScheduledExecutorService executor) {
		this.scheduledExecutorService = executor;
		return this;
	}

	public PackagerInterruptSupplier build() {
		
		if(deadline == Long.MAX_VALUE || deadline == -1L) {
			// no deadline
			if(interrupt != null) {
				return interrupt;
			}
			return NEGATIVE;
		}

		long delay = deadline - System.currentTimeMillis();
		if(delay <= 0) {
			return POSITIVE; // i.e. time is already up
		}

		if(scheduledExecutorService == null) {
			throw new IllegalStateException("Expected scheduler");
		}
				
		if(interrupt == null) {
			DeadlineCheckPackagerInterruptSupplier supplier = new DeadlineCheckPackagerInterruptSupplier();
			ScheduledFuture<?> schedule = scheduledExecutorService.schedule(supplier, delay, TimeUnit.MILLISECONDS);
			supplier.setFuture(schedule);
			return supplier;
		}
		
		DelegateDeadlineCheckPackagerInterruptSupplier supplier = new DelegateDeadlineCheckPackagerInterruptSupplier(interrupt);
		ScheduledFuture<?> schedule = scheduledExecutorService.schedule(supplier, delay, TimeUnit.MILLISECONDS);
		supplier.setFuture(schedule);
		return supplier;
	}

}
