package com.github.skjolber.packing.deadline;

import java.io.Closeable;

@FunctionalInterface
public interface PackagerInterruptSupplier extends Closeable {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 */
	boolean getAsBoolean();
	
	default void close() {
	}
}
