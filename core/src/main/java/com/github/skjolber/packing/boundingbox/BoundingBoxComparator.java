package com.github.skjolber.packing.boundingbox;

/**
 * Orders complete envelopes: negative means the first envelope is preferred.
 * Primitive comparisons and conservative branch bounds can be overridden without
 * search code recognizing particular implementations or singleton instances.
 */
@FunctionalInterface
public interface BoundingBoxComparator {
	int compare(BoundingBox left, BoundingBox right);

	/** Compare a complete candidate without allocating an envelope in specialized implementations. */
	default int compare(int dx, int dy, int dz, BoundingBox right) {
		return compare(new BoundingBox(dx, dy, dz), right);
	}

	/**
	 * Whether some extension of these partial extents might improve the incumbent.
	 * Return false only when every extension is guaranteed not to improve it.
	 * Unknown/custom orderings default to no pruning. Search never uses this bound
	 * while an arbitrary acceptance predicate is still pending.
	 */
	default boolean canImprove(int dx, int dy, int dz, BoundingBox best) {
		return true;
	}
}
