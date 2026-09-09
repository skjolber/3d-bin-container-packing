package com.github.skjolber.packing.api.interrupt;

public interface ClonablePackagerInterruptSupplier extends PackagerInterruptSupplier, Cloneable {

	public ClonablePackagerInterruptSupplier clone();

	public long preventOptmisation();
}
