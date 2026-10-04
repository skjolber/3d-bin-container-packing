package com.github.skjolber.packing.api.interrupt;

public interface CopyablePackagerInterruptSupplier extends PackagerInterruptSupplier {

	public CopyablePackagerInterruptSupplier copy();

	public long preventOptmisation();
}
