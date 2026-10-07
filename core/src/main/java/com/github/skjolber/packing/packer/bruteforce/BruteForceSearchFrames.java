package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;

import org.eclipse.collections.api.iterator.IntIterator;

/**
 * Per-level state of the brute-force placement search ({@link AbstractBruteForcePackager#searchOrder}), kept
 * in arrays instead of on the thread's stack. Level {@code i} is box {@code i} of the order.
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

	/** Whether the box of the level is not placed (see {@link AbstractBruteForcePackager#searchOrder}) */
	final boolean[] unplaced;
	/** The level which entered the level */
	final int[] parents;
	/** The number of boxes placed before the level */
	final int[] placedCounts;
	/** The volume of the boxes placed before the level */
	final long[] placedVolumes;
	/** The highest container priority which may be placed at the level */
	final int[] maxContainerPriorities;
	/** The box (index in the iterator's box items) of each placement */
	final int[] placedPermutations;
	/** The rotation of each placement */
	final int[] placedRotations;
	/** The volume of the boxes from the level on */
	final long[] remainingVolumes;
	/** The smallest area of the boxes from the level on, in any rotation */
	final long[] minAreas;
	/** The box of the level */
	final Box[] boxes;
	final RemainingBoxItem[] items;
	/** the rotations of each level's box which fit the container */
	final BoxStackValue[][] stackValues;

	BruteForceSearchFrames(int levels) {
		this.unplaced = new boolean[levels + 1];
		this.parents = new int[levels + 1];
		this.placedCounts = new int[levels + 1];
		this.placedVolumes = new long[levels + 1];
		this.maxContainerPriorities = new int[levels + 1];
		this.placedPermutations = new int[levels + 1];
		this.placedRotations = new int[levels + 1];
		this.remainingVolumes = new long[levels + 1];
		this.minAreas = new long[levels + 1];
		this.boxes = new Box[levels + 1];
		this.items = new RemainingBoxItem[levels + 1];
		this.stackValues = new BoxStackValue[levels + 1][];
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
