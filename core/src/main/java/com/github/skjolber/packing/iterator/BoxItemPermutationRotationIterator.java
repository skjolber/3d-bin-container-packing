package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

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
 * 			BoxStackValue stackValue = instance.getStackValue(i);
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

public interface BoxItemPermutationRotationIterator {

	/**
	 * 
	 * Get current length
	 * 
	 * @return current length of permutations array
	 */

	int length();

	/**
	 * Back to the first permutation and rotations, so that a packaging attempt iterates all of them: an attempt leaves
	 * the iterator at its last permutation and rotations.
	 */
	void reset();

	BoxStackValue getStackValue(int index);

	/**
	 * @param index position in the current permutation
	 * @return the box item at the position
	 */
	BoxItem getBoxItem(int index);

	/**
	 * @param index position in the current permutation
	 * @return the rotations of the box at the position which fit the container (stack values of its box)
	 */
	BoxStackValue[] getStackValues(int index);

	/**
	 * @return the rotations of each box item which fit the container, by box item index (see {@link #getBoxItems()})
	 */
	BoxStackValue[][] getBoxItemStackValues();

	/**
	 * Get current state
	 * 
	 * @return current state
	 */

	PermutationRotationState getState();

	/**
	 * 
	 * Get permutations + rotations for a state
	 * 
	 * @param state  previously saved state
	 * @param length number of items
	 * @return current permutations + rotations
	 */

	List<BoxStackValue> get(PermutationRotationState state, int length);

	long getMinBoxVolume(int index);

	long[] getMinBoxVolume();

	/**
	 * Get current permutations
	 * 
	 * @return current permutations array
	 */

	int[] getPermutations();
	
	long countRotations();
	
	long countPermutations();

	// write access methods
	

	/**
	 * Next rotation.
	 * 
	 * @return lowest change index, or -1 if none
	 */

	int nextRotation();

	/**
	 * Next rotation. Returns the index of the lowest element which as affected.
	 *
	 * @param maxIndex skip ahead so that rotation affects the argument at index or lower.
	 * @return lowest changed index, or -1 if none
	 */

	int nextRotation(int maxIndex);

	/**
	 * Next permutation.
	 * 
	 * @return lowest change index, or -1 if none
	 */

	int nextPermutation();

	/**
	 * Next permutation. Returns the index of the lowest element which as affected.
	 *
	 * @param maxIndex skip ahead so that permutation affects the argument at index or lower.
	 * @return lowest change index, or -1 if none
	 */

	int nextPermutation(int maxIndex);


	/**
	 * Remove permutations, if present.
	 * 
	 * @param removed list of permutation indexes to remove
	 */

	void removePermutations(List<Integer> removed);
	
	void removePermutations(int count);

	BoxItem[] getBoxItems();
	
}