package com.github.skjolber.packing.packer.bruteforce.reference;

/**
 * Ported from master @ e79cf716 (4.2.4) as a reference implementation for differential tests. The recursion and
 * enumeration order are intentionally preserved - do not modernize or optimize.
 * <p>
 * Origin: {@code com.github.skjolber.packing.iterator.PermutationRotationState}. Capture of rotation and permutation
 * state. The constructor taking a length was dropped (unused by the single-container search).
 */

public class ReferencePermutationRotationState {

	protected int[] rotations; // 2^n or 6^n
	protected int[] permutations; // n!

	public ReferencePermutationRotationState(int[] rotations, int[] permutations) {
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
