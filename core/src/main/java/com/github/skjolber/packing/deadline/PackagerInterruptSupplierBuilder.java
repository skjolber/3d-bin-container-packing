package com.github.skjolber.packing.deadline;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class PackagerInterruptSupplierBuilder {

	public static final NegativePackagerInterruptSupplier NOOP = new NegativePackagerInterruptSupplier();

	private long deadline = Long.MAX_VALUE;
	private BooleanSupplier interrupt = null;
	private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	public static PackagerInterruptSupplierBuilder builder() {
		return new PackagerInterruptSupplierBuilder();
	}

	public PackagerInterruptSupplierBuilder withDeadline(long deadline) {
		this.deadline = deadline;
		return this;
	}

	public PackagerInterruptSupplierBuilder withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}
	
	public PackagerInterruptSupplierBuilder withScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor executor) {
		this.scheduledThreadPoolExecutor = executor;
		return this;
	}

	public PackagerInterruptSupplier build() {
		if(deadline == Long.MAX_VALUE || deadline == -1L) {
			// no deadline
			if(interrupt != null) {
				return new DefaultPackagerInterrupt(interrupt);
			}
			return NOOP;
		}
		
		if(scheduledThreadPoolExecutor == null) {
			throw new IllegalStateException("Expected scheduler");
		}
		
		if(interrupt == null) {
			DeadlineCheckPackagerInterruptSupplier supplier = new DeadlineCheckPackagerInterruptSupplier();
			ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.schedule(supplier, deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			supplier.setFuture(schedule);
			return supplier;
		}
		
		DelegateDeadlineCheckPackagerInterruptSupplier supplier = new DelegateDeadlineCheckPackagerInterruptSupplier(interrupt);
		ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.schedule(supplier, deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		supplier.setFuture(schedule);
		return supplier;
	}

}
