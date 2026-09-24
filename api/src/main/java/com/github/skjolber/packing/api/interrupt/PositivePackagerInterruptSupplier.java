package com.github.skjolber.packing.api.interrupt;

public class PositivePackagerInterruptSupplier implements PackagerInterruptSupplier {

	@Override
	public boolean getAsBoolean() {
		return true;
	}

}
