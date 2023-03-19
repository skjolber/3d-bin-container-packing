package com.github.skjolber.packing.iterator;

import java.util.List;

/**
 *
 * Rotation and permutations built into the same interface. Minimizes the number of
 * rotations. <br>
 * <br>
 * The maximum number of combinations is n! * 6^n, however after accounting for
 * bounds and sides with equal lengths the number can be a lot lower (and this
 * number can be obtained before starting the calculation). <br>
 * <br>
 * Note that permutations are for the boxes which actually fit within this container.
 * <br>
 * <br>
 * Assumes a do-while approach:
 *
 * <pre>
 * {@code
 * do {
 * 	do {
 * 		for (int i = 0; i < n; i++) {
 * 			PermutationRotation box = instance.get(i);
 * 			// .. your code here
 * 		}
 * 	} while (instance.nextRotation() != -1);
 * } while (instance.nextPermutation() != -1);
 *
 * }
 * </pre>
 *
 * @see <a href=
 *      "https://www.nayuki.io/page/next-lexicographical-permutation-algorithm"
 *      target="_top">next-lexicographical-permutation-algorithm</a>
 */

public interface PermutationRotationIterator {

	/**
	 * Get current permutations
	 * 
	 * @return
	 */

	int[] getPermutations();

	/**
	 * 
	 * Get current length
	 * 
	 * @return
	 */

	int length();

	PermutationRotation get(int index);

	/**
	 * Next rotation.
	 * 
	 * @return change index, or -1 if none
	 */

	int nextRotation();

	/**
	 * Next rotation. Returns the index of the lowest element which as affected.
	 *
	 * @param maxIndex skip ahead so that rotation affects the argument at index or lower.
	 * @return change index, or -1 if none
	 */

	int nextRotation(int maxIndex);

	/**
	 * Next permutation.
	 * 
	 * @return change index, or -1 if none
	 */

	int nextPermutation();

	/**
	 * Next permutation. Returns the index of the lowest element which as affected.
	 *
	 * @param maxIndex skip ahead so that permutation affects the argument at index or lower.
	 * @return change index, or -1 if none
	 */

	int nextPermutation(int maxIndex);

	/**
	 * Get current state
	 * 
	 * @return current state
	 */

	PermutationRotationState getState();

	/**
	 * Remove permutations, if present.
	 * 
	 * @param removed list of permutation indexes to remove
	 */

	void removePermutations(List<Integer> removed);

	/**
	 * 
	 * Get permutations + rotations for a state
	 * 
	 * @param state  previously saved state
	 * @param length number of items
	 * @return
	 */

	List<PermutationRotation> get(PermutationRotationState state, int length);

	long getMinStackableVolume();

	long getMinStackableArea();

	int getMinStackableVolumeIndex(int i);

	int getMinStackableAreaIndex(int i);
}