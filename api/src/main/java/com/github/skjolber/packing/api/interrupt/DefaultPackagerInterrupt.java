package com.github.skjolber.packing.api.interrupt;

import java.util.function.BooleanSupplier;

public class DefaultPackagerInterrupt implements PackagerInterruptSupplier {

	private final BooleanSupplier booleanSupplier;

	public DefaultPackagerInterrupt(BooleanSupplier booleanSupplier) {
		this.booleanSupplier = booleanSupplier;
	}

	@Override
	public boolean getAsBoolean() {
		return booleanSupplier.getAsBoolean();
	}

}
