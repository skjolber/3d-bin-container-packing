package com.github.skjolber.packing.deadline;

public interface ClonablePackagerInterruptSupplier extends PackagerInterruptSupplier, Cloneable {

	public ClonablePackagerInterruptSupplier clone();

	public long preventOptmisation();
}
