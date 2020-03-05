package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.Box;

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
* <pre>{@code
* do {
* 	do {
* 		for (int i = 0; i < n; i++) {
* 			Box box = instance.get(i);
* 			// .. your code here
* 		}
* 	} while (instance.nextRotation() != -1);
* } while (instance.nextPermutation() != -1);
*
* }</pre>
*
* @see <a href=
*      "https://www.nayuki.io/page/next-lexicographical-permutation-algorithm"
*      target="_top">next-lexicographical-permutation-algorithm</a>
*/

public interface PermutationRotationIterator extends PermutationSet {

	int[] getPermutations();

	int length();
	
	Box get(int index);

	/**
	 * Next rotation.
	 * 
	 * @return change index, or -1 if none
	 */
	
	int nextRotation();

	/**
	 * Next permutation.
	 * 
	 * @return change index, or -1 if none
	 */

	int nextPermutation();

	PermutationRotationState getState();

	void setState(PermutationRotationState state);

}