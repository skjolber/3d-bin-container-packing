package com.github.skjolber.packing.api;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 */

public interface Packager<B extends PackagerResultBuilder<B>> {

	B newResultBuilder();

}
