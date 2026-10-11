package com.github.skjolber.packing.packer.bruteforce.reference;

import com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * The reference iterator which visits one representative of each permutation and its reverse: the permutation which
 * is not after its reverse in lexicographic order. 4.x does not skip reverse permutations: this is an independent statement of the
 * reverse-equivalence which skipping reverse permutations is based on, for tests of the 5.0 skipping, on top of the 4.x iterator.
 * <p>
 * Skipping ahead with a maximum index (see {@link DefaultBoxItemPermutationRotationIterator#nextPermutation(int)}) is only
 * valid from a permutation which was packed, as all permutations which share its prefix then pack the same. So after a
 * permutation which is skipped for being after its reverse, the following permutation is the next one in order.
 */
public class ReferenceSkippingPermutationRotationIterator extends DefaultBoxItemPermutationRotationIterator {

	public ReferenceSkippingPermutationRotationIterator(DefaultBoxItemPermutationRotationIterator source) {
		super(source.getBoxItems(), source.getExcluded());
	}

	/**
	 * @return true if the current permutation is not after its reverse
	 */
	public boolean isCanonical() {
		int n = permutations.length;
		for (int i = 0; i < n / 2; i++) {
			if(permutations[i] != permutations[n - 1 - i]) {
				return permutations[i] < permutations[n - 1 - i];
			}
		}
		// a palindrome
		return true;
	}

	@Override
	public int nextPermutation(int maxIndex) {
		int result = super.nextPermutation(maxIndex);
		while (result != -1 && !isCanonical()) {
			result = super.nextPermutation();
		}
		return result;
	}

	@Override
	public int nextPermutation() {
		int result = super.nextPermutation();
		while (result != -1 && !isCanonical()) {
			result = super.nextPermutation();
		}
		return result;
	}

}
