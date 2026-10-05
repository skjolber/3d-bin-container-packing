package com.github.skjolber.packing.api;

/**
 * The order in which box items (or box item groups) are inserted, see {@link InsertionOrder}.
 */
public enum Order {

	/** In the given order; packing stops at the first box which does not fit. */
	CHRONOLOGICAL,
	/** In the given order, skipping boxes which do not fit. */
	CHRONOLOGICAL_ALLOW_SKIPPING,
	/** In any order. */
	NONE

}
