package com.github.skjolber.packing.packer.bruteforce.flat;

/**
 * The winning arrangement which a {@linkplain BruteForceAccelerator} found: the best of all permutations of the boxes of a {@linkplain FlatProblem}, by
 * load volume, then load weight, then number of boxes, where the first permutation wins a tie. The packager places the winner again with its own search,
 * so that the positions are those of the packager, and checks the count and the load volume of the result against the winner's.
 */
public final class AcceleratorWinner {

	private final int[] permutation;
	private final int[] rotations;
	private final int count;
	private final long loadVolume;
	private final long loadWeight;
	private final long nodes;
	private final boolean fullyPacked;

	/**
	 * @param permutation the box item (type) of each box, in the order of the winning permutation: all boxes, also those which were not placed
	 * @param rotations the rotation of each placed box, as an index in the rotations of its box item
	 * @param count the number of placed boxes, which is zero if no box could be placed
	 * @param loadVolume the load volume of the placed boxes
	 * @param loadWeight the load weight of the placed boxes
	 * @param nodes the number of search nodes the accelerator visited (statistics)
	 * @param fullyPacked true if all boxes were placed
	 */
	public AcceleratorWinner(int[] permutation, int[] rotations, int count, long loadVolume, long loadWeight, long nodes, boolean fullyPacked) {
		this.permutation = permutation;
		this.rotations = rotations;
		this.count = count;
		this.loadVolume = loadVolume;
		this.loadWeight = loadWeight;
		this.nodes = nodes;
		this.fullyPacked = fullyPacked;
	}

	/** @return the box item (type) of each box, in the order of the winning permutation */
	public int[] getPermutation() {
		return permutation;
	}

	/** @return the rotation of each placed box (at least the first {@link #getCount()} values are valid) */
	public int[] getRotations() {
		return rotations;
	}

	/** @return the number of placed boxes, or zero if no box could be placed */
	public int getCount() {
		return count;
	}

	public long getLoadVolume() {
		return loadVolume;
	}

	public long getLoadWeight() {
		return loadWeight;
	}

	/** @return the number of search nodes visited */
	public long getNodes() {
		return nodes;
	}

	public boolean isFullyPacked() {
		return fullyPacked;
	}
}
