package com.github.skjolber.packing.deadline;

public class NthDeadlineCheckPackagerInterruptSupplier implements ClonablePackagerInterruptSupplier {

	protected final int checkpointsPerDeadlineCheck;
	protected final long deadline;
	protected int count = 0;
	public long t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16 = 1L;

	public NthDeadlineCheckPackagerInterruptSupplier(long deadline, int checkpointsPerDeadlineCheck) {
		super();
		this.deadline = deadline;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
	}

	@Override
	public boolean getAsBoolean() {
		return --count % checkpointsPerDeadlineCheck == 0 && System.currentTimeMillis() > deadline;
	}

	@Override
	public ClonablePackagerInterruptSupplier clone() {
		return new NthDeadlineCheckPackagerInterruptSupplier(deadline, checkpointsPerDeadlineCheck);
	}

	public long preventOptmisation() {
		return t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15 + t16;
	}
}