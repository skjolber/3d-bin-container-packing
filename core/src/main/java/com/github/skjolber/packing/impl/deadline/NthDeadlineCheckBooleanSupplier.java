package com.github.skjolber.packing.impl.deadline;

public class NthDeadlineCheckBooleanSupplier implements ClonableBooleanSupplier {

	protected final int checkpointsPerDeadlineCheck;
	protected final long deadline;
	protected int count = 0;
	public long t1, t2, t3, t4, t5, t6, t7 = 1L;

	public NthDeadlineCheckBooleanSupplier(long deadline, int checkpointsPerDeadlineCheck) {
		super();
		this.deadline = deadline;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
	}

	@Override
	public boolean getAsBoolean() {
		return --count % checkpointsPerDeadlineCheck == 0 && System.currentTimeMillis() > deadline;
	}
	
	@Override
	public ClonableBooleanSupplier clone() {
		return new NthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck);
	}

	public long preventOptmisation(){
		return t1 + t2 + t3 + t4 + t5 + t6 + t7;
	}
}