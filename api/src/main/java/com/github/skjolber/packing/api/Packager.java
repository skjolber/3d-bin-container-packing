package com.github.skjolber.packing.api;

import java.io.Closeable;

/**
 * Fit boxes into containers, i.e. perform bin packing over one or more containers.
 * <p>
 * A packager creates {@linkplain PackagerResultBuilder}s, which are configured with the boxes and containers, and then build
 * the result. Packager instances are thread-safe; result builders are not, so use one per packaging.
 * <p>
 * {@linkplain #close()} releases the resources of the packager, such as its threads.
 *
 * @param <B> result builder type
 */

public interface Packager<B extends PackagerResultBuilder> extends Closeable {

	/**
	 * Create a result builder for a new packaging.
	 *
	 * @return a new result builder, not thread-safe
	 */
	B newResultBuilder();

}
