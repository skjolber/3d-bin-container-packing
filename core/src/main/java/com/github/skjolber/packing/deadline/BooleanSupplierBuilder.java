package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class BooleanSupplierBuilder {
	
	public static final BooleanSupplier NOOP = () -> false;
	
	private long deadline = Long.MAX_VALUE;
	private int checkpointsPerDeadlineCheck = 1;
	private BooleanSupplier interrupt = NOOP;
	
	public static BooleanSupplierBuilder builder() {
		return new BooleanSupplierBuilder ();
	}
	
	public BooleanSupplierBuilder withDeadline(long deadline, int checkpointsPerDeadlineCheck) {
		this.deadline = deadline;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
		return this;
	}
	
	public BooleanSupplierBuilder withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}

	public BooleanSupplier build() {
		if(checkpointsPerDeadlineCheck == Integer.MAX_VALUE || deadline == Long.MAX_VALUE) {
			// no deadline
			return interrupt;
		}
		
		if(checkpointsPerDeadlineCheck == 1) {
			if(interrupt == NOOP) {
				return new DeadlineCheckBooleanSupplier(deadline);
			}
			return new DelegateDeadlineCheckBooleanSupplier(deadline, interrupt);
		}
		if(interrupt == NOOP) {
			//return new DeadlineCheckBooleanSupplier(deadline);
			return new NthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck);
		}
		return new DelegateNthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck, interrupt);
	}

}
