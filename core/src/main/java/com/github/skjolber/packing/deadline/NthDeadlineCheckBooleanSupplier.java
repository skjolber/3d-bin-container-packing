package com.github.skjolber.packing.deadline;

@jdk.internal.vm.annotation.Contended
public class NthDeadlineCheckBooleanSupplier implements ClonableBooleanSupplier {

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
	public ClonableBooleanSupplier clone() {
		return new NthDeadlineCheckBooleanSupplier(deadline, checkpointsPerDeadlineCheck);
	}

}