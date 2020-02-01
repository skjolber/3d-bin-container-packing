package com.github.skjolber.packing.impl;

import java.util.function.BooleanSupplier;

public class NthBooleanSupplier implements BooleanSupplier {

	private final BooleanSupplier delegate;
	private final int n;
	private int count = 0;
	
	public NthBooleanSupplier(BooleanSupplier delegate, int n) {
		super();
		this.delegate = delegate;
		this.n = n;
	}

	@Override
	public boolean getAsBoolean() {
		count++;
		if(n % count == 0) {
			return delegate.getAsBoolean();
		}
		return false;
	}
	
}