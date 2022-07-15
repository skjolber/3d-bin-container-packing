package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

@jdk.internal.vm.annotation.Contended
public class DelegateNthDeadlineCheckBooleanSupplier implements ClonableBooleanSupplier {

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
		return delegate.getAsBoolean() || (--count % checkpointsPerDeadlineCheck == 0 && System.currentTimeMillis() > deadline);
	}
	
	@Override
	public ClonableBooleanSupplier clone() {
		return new DelegateNthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck, delegate);
	}

}