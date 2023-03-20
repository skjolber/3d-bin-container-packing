package com.github.skjolber.packing.deadline;

import java.util.function.BooleanSupplier;

public class NegativePackagerInterruptSupplier implements PackagerInterruptSupplier {
	
	@Override
	public boolean getAsBoolean() {
		return false;
	}

}
