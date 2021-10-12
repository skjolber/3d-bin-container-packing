package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.deadline.ClonableBooleanSupplier;

public interface ClonableBooleanSupplier extends BooleanSupplier, Cloneable {

	public ClonableBooleanSupplier clone();
	
	public long preventOptmisation();
}
