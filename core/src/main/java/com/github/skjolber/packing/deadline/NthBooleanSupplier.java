package com.github.skjolber.packing.deadline;

import java.util.concurrent.atomic.AtomicBoolean;

public class NthBooleanSupplier implements ClonableBooleanSupplier {

	protected final int checkpointsPerDeadlineCheck;
	protected int count = 0;
	public long t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16 = 1L;
	protected AtomicBoolean atomicBoolean;

	public NthBooleanSupplier(AtomicBoolean atomicBoolean, int checkpointsPerDeadlineCheck) {
		super();
		this.atomicBoolean = atomicBoolean;
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
	}

	@Override
	public boolean getAsBoolean() {
		return --count % checkpointsPerDeadlineCheck == 0 && atomicBoolean.get();
	}
	
	@Override
	public ClonableBooleanSupplier clone() {
		return new NthBooleanSupplier(atomicBoolean, checkpointsPerDeadlineCheck);
	}

	public long preventOptmisation(){
		return t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15 + t16;
	}
}