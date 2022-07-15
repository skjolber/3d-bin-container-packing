package com.github.skjolber.packing.deadline;

import java.util.concurrent.atomic.AtomicBoolean;

@jdk.internal.vm.annotation.Contended
public class NthBooleanSupplier implements ClonableBooleanSupplier {

	protected final int checkpointsPerDeadlineCheck;
	protected int count = 0;
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

}