package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class NegativeBooleanSupplier implements BooleanSupplier {
	
	@Override
	public boolean getAsBoolean() {
		return false;
	}

}
