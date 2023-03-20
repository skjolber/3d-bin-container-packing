package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class PackagerInterruptSupplierBuilder {

	public static final NegativePackagerInterruptSupplier NOOP = new NegativePackagerInterruptSupplier();

	private long deadline = Long.MAX_VALUE;
	private int checkpointsPerDeadlineCheck = 1;
	private BooleanSupplier interrupt = null;

	public static PackagerInterruptSupplierBuilder builder() {
		return new PackagerInterruptSupplierBuilder();
	}

	public PackagerInterruptSupplierBuilder withDeadline(long deadline, int checkpointsPerDeadlineCheck) {
		this.deadline = deadline;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
		return this;
	}

	public PackagerInterruptSupplierBuilder withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}

	public PackagerInterruptSupplier build() {
		if(checkpointsPerDeadlineCheck == Integer.MAX_VALUE || checkpointsPerDeadlineCheck == -1 || deadline == Long.MAX_VALUE || deadline == -1L) {
			// no deadline
			if(interrupt != null) {
				return new DefaultPackagerInterrupt(interrupt);
			}
			return NOOP;
		}

		if(checkpointsPerDeadlineCheck == 1) {
			if(interrupt == null) {
				return new DeadlineCheckPackagerInterruptSupplier(deadline);
			}
			return new DelegateDeadlineCheckPackagerInterruptSupplier(deadline, interrupt);
		}
		if(interrupt == null) {
			return new NthDeadlineCheckPackagerInterruptSupplier(deadline, checkpointsPerDeadlineCheck);
		}
		return new DelegateNthDeadlineCheckPackagerInterruptSupplier(deadline, checkpointsPerDeadlineCheck, interrupt);
	}

}
