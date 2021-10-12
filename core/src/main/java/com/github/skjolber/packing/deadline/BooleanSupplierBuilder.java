package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.api.deadline.DeadlineCheckBooleanSupplier;
import com.github.skjolber.packing.api.deadline.DelegateDeadlineCheckBooleanSupplier;
import com.github.skjolber.packing.api.deadline.DelegateNthDeadlineCheckBooleanSupplier;
import com.github.skjolber.packing.api.deadline.NthDeadlineCheckBooleanSupplier;

public class BooleanSupplierBuilder {
	
	private static final BooleanSupplier noop = () -> false;
	
	private long deadline = Long.MAX_VALUE;
	private int checkpointsPerDeadlineCheck = 1;
	private BooleanSupplier interrupt = noop;
	
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
			if(interrupt == noop) {
				return new DeadlineCheckBooleanSupplier(deadline);
			}
			return new DelegateDeadlineCheckBooleanSupplier(deadline, interrupt);
		}
		if(interrupt == noop) {
			//return new DeadlineCheckBooleanSupplier(deadline);
			return new NthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck);
		}
		return new DelegateNthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck, interrupt);
	}

}
