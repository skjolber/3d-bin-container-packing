package com.github.skjolber.packing.api;

import java.io.Closeable;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 */

public interface Packager<B extends PackagerResultBuilder> extends Closeable {

	B newResultBuilder();

}
