package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public interface ClonableBooleanSupplier extends BooleanSupplier, Cloneable {

	public ClonableBooleanSupplier clone();
}
