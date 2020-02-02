package com.github.skjolber.packing.impl.deadline;

import java.util.function.BooleanSupplier;

public class DelegateDeadlineCheckBooleanSupplier implements BooleanSupplier {

	protected final BooleanSupplier delegate;
	protected final long deadline;
	
	public DelegateDeadlineCheckBooleanSupplier(long deadline, BooleanSupplier delegate) {
		super();
		this.deadline = deadline;
		this.delegate = delegate;
	}

	@Override
	public boolean getAsBoolean() {
		return delegate.getAsBoolean() || System.currentTimeMillis() > deadline;
	}
	
}