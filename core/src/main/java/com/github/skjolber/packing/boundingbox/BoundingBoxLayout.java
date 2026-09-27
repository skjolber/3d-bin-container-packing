package com.github.skjolber.packing.boundingbox;

import com.github.skjolber.packing.api.Stack;

/**
 * A retained complete arrangement, independent of the mutable search state.
 * Multiple objectives may reference the same layout when it wins them together.
 * The returned stack is mutable; callers should not mutate shared results.
 */
public class BoundingBoxLayout {

	protected final BoundingBox boundingBox;
	protected final Stack stack;

	protected BoundingBoxLayout(BoundingBox boundingBox, Stack stack) {
		this.boundingBox = boundingBox;
		this.stack = stack;
	}

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	/** Original input identities and, for load-aware search, a complete support graph. */
	public Stack getStack() {
		return stack;
	}
}
