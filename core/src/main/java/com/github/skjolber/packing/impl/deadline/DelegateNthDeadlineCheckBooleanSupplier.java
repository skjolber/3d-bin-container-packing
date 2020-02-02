package com.github.skjolber.packing.impl.deadline;

import java.util.function.BooleanSupplier;

public class DelegateNthDeadlineCheckBooleanSupplier implements BooleanSupplier {

	protected final BooleanSupplier delegate;
	protected final int checkpointsPerDeadlineCheck;
	protected final long deadline;
	protected int count = 0;
	
	public DelegateNthDeadlineCheckBooleanSupplier(long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier delegate) {
		super();
		this.deadline = deadline;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
		this.delegate = delegate;
	}

	@Override
	public boolean getAsBoolean() {
		return (--count % checkpointsPerDeadlineCheck == 0 && System.currentTimeMillis() > deadline) ||  delegate.getAsBoolean();
	}
	
	@Override
	public Object clone() {
		return new DelegateNthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck, delegate);
	}
	
}