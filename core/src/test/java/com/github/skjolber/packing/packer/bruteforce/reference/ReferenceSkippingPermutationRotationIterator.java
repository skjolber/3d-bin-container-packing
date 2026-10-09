package com.github.skjolber.packing.packer.bruteforce.reference;

/**
 * The reference iterator which visits one representative of each permutation and its reverse: the permutation which
 * is not after its reverse in lexicographic order. This is not a port, but an independent statement of the
 * reverse-equivalence which skipping reverse permutations is based on, for tests of the 5.0 skipping.
 * <p>
 * Skipping ahead with a maximum index (see {@link ReferencePermutationRotationIterator#nextPermutation(int)}) is only
 * valid from a permutation which was packed, as all permutations which share its prefix then pack the same. So after a
 * permutation which is skipped for being after its reverse, the following permutation is the next one in order.
 */

public class ReferenceSkippingPermutationRotationIterator extends ReferencePermutationRotationIterator {

	public ReferenceSkippingPermutationRotationIterator(ReferencePermutationRotationIterator source) {
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
