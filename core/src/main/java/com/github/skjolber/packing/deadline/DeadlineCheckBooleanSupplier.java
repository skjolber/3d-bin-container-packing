package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class DeadlineCheckBooleanSupplier implements BooleanSupplier {

	protected final long deadline;

	public DeadlineCheckBooleanSupplier(long deadline) {
		super();
		this.deadline = deadline;
	}

	@Override
	public boolean getAsBoolean() {
		return System.currentTimeMillis() > deadline;
	}

}