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
	/** The index of the remaining box with the smallest area, which sets the point calculator's minimum point area. */
	final int[] minStackableAreaIndexes;
	/** The container's max load weight minus the weight of the boxes placed at earlier levels. */
	final int[] freeLoadWeights;
	/** The rotation of the box at the level, in the search in a box item order (which tries the rotations). */
	final int[] rotationIndexes;
	/** The positions where the box at the level is fully supported, when full support is required (created on first use). */
	private final FullSupportCandidates[] fullSupportCandidates;

	BruteForceSearchFrames(int levels) {
		this.nextPointIndexes = new int[levels];
		this.pointCounts = new int[levels];
		this.pointIterators = new IntIterator[levels];
		this.minStackableAreaIndexes = new int[levels];
		this.freeLoadWeights = new int[levels];
		this.rotationIndexes = new int[levels];
		this.fullSupportCandidates = new FullSupportCandidates[levels];
	}

	FullSupportCandidates getFullSupportCandidates(int level) {
		FullSupportCandidates candidates = fullSupportCandidates[level];
		if(candidates == null) {
			candidates = new FullSupportCandidates();
			fullSupportCandidates[level] = candidates;
		}
		return candidates;
	}
}
