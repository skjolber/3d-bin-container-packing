package com.github.skjolber.packing.impl;

/**
 * Capture of rotation and permutation state.
 *
 */

public class PermutationRotationState {

	private int[] rotations; // 2^n or 6^n
	private int[] permutations; // n!

	PermutationRotationState(int[] rotations, int[] permutations) {
		super();
		this.rotations = new int[rotations.length];
		System.arraycopy(rotations, 0, this.rotations, 0, rotations.length);
		this.permutations = new int[permutations.length];
		System.arraycopy(permutations, 0, this.permutations, 0, permutations.length);
	}
	public int[] getPermutations() {
		return permutations;
	}
	public int[] getRotations() {
		return rotations;
	}

}
