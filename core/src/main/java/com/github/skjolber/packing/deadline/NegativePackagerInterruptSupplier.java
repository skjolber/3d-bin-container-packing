package com.github.skjolber.packing.deadline;

public class NegativePackagerInterruptSupplier implements PackagerInterruptSupplier {

	@Override
	public boolean getAsBoolean() {
		return false;
	}

}
