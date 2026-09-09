package com.github.skjolber.packing.iterator;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * A decorator which visits one representative of each pair consisting of a
 * permutation and its reverse. Palindromic permutations are visited once.
 *
 * <p>
 * The lexicographically smaller member of each pair is retained. Rotations are
 * not filtered.
 * </p>
 */
public class FilteredReversedBoxItemPermutationRotationIterator implements BoxItemPermutationRotationIterator {

	private final BoxItemPermutationRotationIterator iterator;

	public FilteredReversedBoxItemPermutationRotationIterator(BoxItemPermutationRotationIterator iterator) {
		if (iterator == null) {
			throw new IllegalArgumentException("Iterator cannot be null");
		}
		this.iterator = iterator;
	}

	public BoxItemPermutationRotationIterator getIterator() {
		return iterator;
	}

	@Override
	public int nextPermutation() {
		return nextPermutationInternal(-1);
	}

	@Override
	public int nextPermutation(int maxIndex) {
		return nextPermutationInternal(maxIndex);
	}

	private int nextPermutationInternal(int maxIndex) {
		int[] previous = iterator.getPermutations();
		while (true) {
			int result = maxIndex == -1 ? iterator.nextPermutation() : iterator.nextPermutation(maxIndex);
			if (result == -1) {
				return -1;
			}

			int[] current = iterator.getPermutations();
			if (isCanonical(current)) {
				for (int i = 0; i < current.length; i++) {
					if (previous[i] != current[i]) {
						return i;
					}
				}
				throw new IllegalStateException("Delegate advanced without changing its permutation");
			}
		}
	}

	/**
	 * Keep the lexicographically smallest member of a permutation/reverse pair.
	 * If both are equal, the permutation is a palindrome and is also retained.
	 */
	public static boolean isCanonical(int[] permutation) {
		return compareWithReverse(permutation) <= 0;
	}

	/**
	 * Compare a permutation with its reverse without allocating a reversed array.
	 * The first unequal pair determines the lexicographical order.
	 */
	private static int compareWithReverse(int[] permutation) {
		int lastIndex = permutation.length - 1;
		for (int index = 0; index < permutation.length / 2; index++) {
			int value = permutation[index];
			int reversedValue = permutation[lastIndex - index];

			if (value != reversedValue) {
				return Integer.compare(value, reversedValue);
			}
		}
		return 0;
	}

	@Override
	public long countPermutations() {
		long permutations = iterator.countPermutations();
		if (permutations == -1L) {
			return -1L;
		}

		Map<Integer, Integer> frequencies = new HashMap<>();
		for (int value : iterator.getPermutations()) {
			frequencies.merge(value, 1, Integer::sum);
		}

		int odd = 0;
		int halfLength = 0;
		BigInteger palindromes = BigInteger.ONE;
		for (int frequency : frequencies.values()) {
			odd += frequency & 1;
			halfLength += frequency / 2;
		}
		if (odd > 1) {
			return permutations / 2;
		}

		for (int i = 2; i <= halfLength; i++) {
			palindromes = palindromes.multiply(BigInteger.valueOf(i));
		}
		for (int frequency : frequencies.values()) {
			for (int i = 2; i <= frequency / 2; i++) {
				palindromes = palindromes.divide(BigInteger.valueOf(i));
			}
		}
		return BigInteger.valueOf(permutations).add(palindromes).divide(BigInteger.TWO).longValueExact();
	}

	@Override
	public int length() {
		return iterator.length();
	}

	@Override
	public BoxStackValue getStackValue(int index) {
		return iterator.getStackValue(index);
	}

	@Override
	public PermutationRotationState getState() {
		return iterator.getState();
	}

	@Override
	public List<BoxStackValue> get(PermutationRotationState state, int length) {
		return iterator.get(state, length);
	}

	@Override
	public long getMinBoxVolume(int index) {
		return iterator.getMinBoxVolume(index);
	}

	@Override
	public long[] getMinBoxVolume() {
		return iterator.getMinBoxVolume();
	}

	@Override
	public int getMinStackableAreaIndex(int index) {
		return iterator.getMinStackableAreaIndex(index);
	}

	@Override
	public int[] getPermutations() {
		return iterator.getPermutations();
	}

	@Override
	public long countRotations() {
		return iterator.countRotations();
	}

	@Override
	public int nextRotation() {
		return iterator.nextRotation();
	}

	@Override
	public int nextRotation(int maxIndex) {
		return iterator.nextRotation(maxIndex);
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		iterator.removePermutations(removed);
	}

	@Override
	public void removePermutations(int count) {
		iterator.removePermutations(count);
	}

	@Override
	public BoxItem[] getBoxItems() {
		return iterator.getBoxItems();
	}
}
