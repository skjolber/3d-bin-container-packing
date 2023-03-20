package com.github.skjolber.packing.deadline;

public class DeadlineCheckPackagerInterruptSupplier implements PackagerInterruptSupplier {

	protected final long deadline;

	public DeadlineCheckPackagerInterruptSupplier(long deadline) {
		super();
		this.deadline = deadline;
	}

	@Override
	public boolean getAsBoolean() {
		return System.currentTimeMillis() > deadline;
	}

}