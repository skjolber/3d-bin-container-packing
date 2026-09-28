package com.github.skjolber.packing.virtualbox.bounds;

/**
 * Orders complete envelopes: negative means the first envelope is preferred.
 * Primitive comparisons and conservative branch bounds can be overridden without
 * search code recognizing particular implementations or singleton instances.
 */
@FunctionalInterface
public interface VirtualBoxBoundsComparator {
	int compare(VirtualBoxBounds left, VirtualBoxBounds right);

	/** Compare a complete candidate without allocating an envelope in specialized implementations. */
	default int compare(int dx, int dy, int dz, VirtualBoxBounds right) {
		return compare(new VirtualBoxBounds(dx, dy, dz), right);
	}

	/**
	 * Whether some extension of these partial extents might improve the incumbent.
	 * Return false only when every extension is guaranteed not to improve it.
	 * Unknown/custom orderings default to no pruning. Search never uses this bound
	 * while an arbitrary acceptance predicate is still pending.
	 */
	default boolean canImprove(int dx, int dy, int dz, VirtualBoxBounds best) {
		return true;
	}
}
