package com.github.skjolber.packing.impl.deadline;

import java.util.function.BooleanSupplier;

public interface ClonableBooleanSupplier extends BooleanSupplier, Cloneable {

	public ClonableBooleanSupplier clone();
	
	public long preventOptmisation();
}
