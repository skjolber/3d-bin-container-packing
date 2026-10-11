package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * Per-level state of the fast brute-force search ({@link FastBruteForcePackager#searchOrder}), kept in arrays
 * instead of on the thread's stack, and reused between searches by the same {@link FastPointCalculator3DStack}. Level
 * {@code i} is box {@code i} of the order.
 */
final class FastSearchFrames {

	/** Whether the box of the level is not placed: it does not fit, or (when skipping) it is skipped */
	final boolean[] unplaced;
	/** The next rotation to try for the box of the level */
	final int[] rotationIndexes;
	/** The level which entered the level (when skipping, an earlier level than the one before) */
	final int[] parents;
	/** The number of boxes placed before the level */
	final int[] placedCounts;
	/** The volume of the boxes placed before the level */
	final long[] placedVolumes;
	/** The container's max load weight minus the weight of the boxes placed before the level */
	final int[] freeLoadWeights;
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
	final BoxItem[] items;
	/** the rotations of each level's box which fit the container */
	final BoxStackValue[][] stackValues;

	FastSearchFrames(int levels) {
		this.unplaced = new boolean[levels + 1];
		this.rotationIndexes = new int[levels + 1];
		this.parents = new int[levels + 1];
		this.placedCounts = new int[levels + 1];
		this.placedVolumes = new long[levels + 1];
		this.freeLoadWeights = new int[levels + 1];
		this.maxContainerPriorities = new int[levels + 1];
		this.placedPermutations = new int[levels + 1];
		this.placedRotations = new int[levels + 1];
		this.remainingVolumes = new long[levels + 1];
		this.minAreas = new long[levels + 1];
		this.boxes = new Box[levels + 1];
		this.items = new BoxItem[levels + 1];
		this.stackValues = new BoxStackValue[levels + 1][];
	}
}
