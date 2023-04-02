package com.github.skjolber.packing.deadline;

@FunctionalInterface
public interface PackagerInterruptSupplier {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 */
	boolean getAsBoolean();
}
