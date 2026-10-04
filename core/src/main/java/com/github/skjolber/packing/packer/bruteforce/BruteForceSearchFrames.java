package com.github.skjolber.packing.packer.bruteforce;

import org.eclipse.collections.api.iterator.IntIterator;

/**
 * Per-level state of the brute-force placement search ({@link AbstractBruteForcePackager#search}), kept
 * in arrays instead of on the thread's stack. Level {@code i} places box {@code i} of the permutation.
 * Reused between searches by the same {@link PointCalculator3DStack}.
 */
final class BruteForceSearchFrames {

	/** The next point index to try, when trying all fitting points. */
	final int[] nextPointIndexes;
	/** The number of points at the level, when trying all fitting points. */
	final int[] pointCounts;
	/** The remaining candidate points, when using a point filter. */
	final IntIterator[] pointIterators;
	final int[] minStackableAreaIndexes;
	final int[] freeLoadWeights;

	BruteForceSearchFrames(int levels) {
		this.nextPointIndexes = new int[levels];
		this.pointCounts = new int[levels];
		this.pointIterators = new IntIterator[levels];
		this.minStackableAreaIndexes = new int[levels];
		this.freeLoadWeights = new int[levels];
	}
}
