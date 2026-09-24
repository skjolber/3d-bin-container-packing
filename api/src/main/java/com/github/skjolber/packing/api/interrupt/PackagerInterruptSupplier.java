package com.github.skjolber.packing.api.interrupt;

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
