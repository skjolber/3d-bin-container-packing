package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class DelegateNthDeadlineCheckBooleanSupplier implements ClonableBooleanSupplier {

	protected final BooleanSupplier delegate;
	protected final int checkpointsPerDeadlineCheck;
	protected final long deadline;
	protected int count = 0;
	public long t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16 = 1L;
	
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

	public long preventOptmisation(){
		return t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15 + t16;
	}

}