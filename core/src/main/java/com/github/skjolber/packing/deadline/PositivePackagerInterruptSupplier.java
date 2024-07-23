package com.github.skjolber.packing.deadline;

public class PositivePackagerInterruptSupplier implements PackagerInterruptSupplier {

	@Override
	public boolean getAsBoolean() {
		return true;
	}

}
