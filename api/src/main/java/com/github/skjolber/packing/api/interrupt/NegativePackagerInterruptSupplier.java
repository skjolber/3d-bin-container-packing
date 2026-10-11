package com.github.skjolber.packing.api.interrupt;

public class NegativePackagerInterruptSupplier implements PackagerInterruptSupplier {

	@Override
	public boolean getAsBoolean() {
		return false;
	}

}
