package com.github.skjolber.packing.api;

/**
 * The order in which box items (or box item groups) arrive for loading, and so are inserted, see {@link InsertionOrder}.
 * For the order in which boxes are taken out, see {@link BoxItem#withExtractionOrder(int)}, and for which boxes go in
 * earlier containers, {@link BoxItem#withContainerPriority(int)}.
 */
public enum Order {

	/** In the given order; packing stops at the first box which does not fit. */
	CHRONOLOGICAL,
	/** In the given order, skipping boxes which do not fit. */
	CHRONOLOGICAL_ALLOW_SKIPPING,
	/** In any order. */
	NONE

}
