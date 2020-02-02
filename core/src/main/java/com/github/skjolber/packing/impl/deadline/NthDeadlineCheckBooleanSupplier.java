package com.github.skjolber.packing.impl.deadline;

import java.util.function.BooleanSupplier;

public class NthDeadlineCheckBooleanSupplier implements BooleanSupplier {

	protected final int checkpointsPerDeadlineCheck;
	protected final long deadline;
	protected int count = 0;
	
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
	public Object clone() {
		return new NthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck);
	}
	
}