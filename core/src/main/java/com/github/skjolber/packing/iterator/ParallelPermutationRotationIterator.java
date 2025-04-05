package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;

public class ParallelPermutationRotationIterator extends AbstractPermutationRotationIterator {

	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;

	private int[] permutations;
	private int[] rotations;

	private int[] lastPermutation;
	private int lastPermutationMaxIndex;
	private boolean checkLastPermutation = false;

	// minimum volume from index i and above
	protected long[] minStackableVolume;

	// parent iterator
	private ParallelPermutationRotationIteratorList iterator;

	public ParallelPermutationRotationIterator(PermutationStackableValue[] matrix, ParallelPermutationRotationIteratorList iterator) {
		super(matrix);

		this.iterator = iterator;
	}

	public long preventOptmisation() {
		return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
	}

	public void setReset(int[] reset) {
		this.reset = reset;
	}

	public int[] getPermutations() {
		// TODO reuse object
		int[] result = new int[permutations.length - ParallelPermutationRotationIteratorList.PADDING];
		System.arraycopy(permutations, ParallelPermutationRotationIteratorList.PADDING, result, 0, result.length);
		return result;
	}

	public void setPermutations(int[] permutations) {
		this.permutations = permutations;
	}

	public void initMinStackableVolume() {
		this.minStackableVolume = new long[permutations.length]; // i.e. with padding

		calculateMinStackableVolume(0);
	}

	public void calculateMinStackableVolume(int offset) {
		if(permutations.length > ParallelPermutationRotationIteratorList.PADDING) {
			PermutationStackableValue value = matrix[permutations[permutations.length - 1]];
			PermutationRotation last = value.getBoxes()[0];
	
			minStackableVolume[permutations.length - 1] = last.getBoxStackValue().getVolume();
			for (int i = permutations.length - 2; i >= offset + ParallelPermutationRotationIteratorList.PADDING; i--) {
				long volume = matrix[permutations[i]].getBoxes()[0].getBoxStackValue().getVolume();
				if(volume < minStackableVolume[i + 1]) {
					minStackableVolume[i] = volume;
				} else {
					minStackableVolume[i] = minStackableVolume[i + 1];
				}
			}
		}
	}

	public long getMinStackableVolume(int offset) {
		return minStackableVolume[ParallelPermutationRotationIteratorList.PADDING + offset];
	}

	public int[] getRotations() {
		int[] result = new int[rotations.length - ParallelPermutationRotationIteratorList.PADDING];
		System.arraycopy(rotations, ParallelPermutationRotationIteratorList.PADDING, result, 0, result.length);
		return result;
	}

	public void setRotations(int[] rotations) {
		this.rotations = rotations;
	}

	public int[] getLastPermutation() {
		return lastPermutation;
	}

	public void setLastPermutation(int[] lastPermutation) {
		this.lastPermutation = lastPermutation;

		this.checkLastPermutation = false;

		// find the first item that differs, so that we do not have to
		// compare items for each iteration (to detect whether we have done enough work)
		for (int k = 0; k < lastPermutation.length; k++) {
			if(permutations[ParallelPermutationRotationIteratorList.PADDING + k] != lastPermutation[k]) {
				lastPermutationMaxIndex = k;

				break;
			}
		}
	}

	public int getLastPermutationMaxIndex() {
		return lastPermutationMaxIndex;
	}

	public int nextRotation() {
		return nextRotation(rotations.length - 1 - ParallelPermutationRotationIteratorList.PADDING);
	}

	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = ParallelPermutationRotationIteratorList.PADDING + maxIndex; i >= ParallelPermutationRotationIteratorList.PADDING; i--) {
			if(rotations[i] < matrix[permutations[i]].getBoxes().length - 1) {
				rotations[i]++;

				// reset all following counters
				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i - ParallelPermutationRotationIteratorList.PADDING;
			}
		}

		return -1;
	}

	protected int nextPermutationImpl() {
		// https://www.baeldung.com/cs/array-generate-all-permutations#permutations-in-lexicographic-order

		// Find longest non-increasing suffix

		int i = permutations.length - 1;
		while (i > ParallelPermutationRotationIteratorList.PADDING && permutations[i - 1] >= permutations[i])
			i--;
		// Now i is the head index of the suffix

		// Are we at the last permutation already?
		if(i <= ParallelPermutationRotationIteratorList.PADDING) {
			return -1;
		}

		// Let array[i - 1] be the pivot
		// Find rightmost element that exceeds the pivot
		int j = permutations.length - 1;
		while (permutations[j] <= permutations[i - 1])
			j--;
		// Now the value array[j] will become the new pivot
		// Assertion: j >= i

		int head = i - 1 - ParallelPermutationRotationIteratorList.PADDING;

		// Swap the pivot with j
		int temp = permutations[i - 1];
		permutations[i - 1] = permutations[j];
		permutations[j] = temp;

		// Reverse the suffix
		j = permutations.length - 1;
		while (i < j) {
			temp = permutations[i];
			permutations[i] = permutations[j];
			permutations[j] = temp;
			i++;
			j--;
		}

		// Successfully computed the next permutation
		return head;
	}

	public int nextPermutation() {
		resetRotations();

		int resultIndex = nextPermutationImpl();

		int result = returnPermuationWithinRangeOrMinusOne(resultIndex);
		if(result != -1) {
			calculateMinStackableVolume(resultIndex);
		}
		return result;
	}

	public int nextPermutation(int maxIndex) {
		// reset rotations
		resetRotations();

		int resultIndex = nextWorkUnitPermutation(permutations, maxIndex);

		int result = returnPermuationWithinRangeOrMinusOne(resultIndex);
		if(result != -1) {
			calculateMinStackableVolume(resultIndex);
		}
		return result;
	}

	private int returnPermuationWithinRangeOrMinusOne(int resultIndex) {
		if(lastPermutation != null) {
			if(resultIndex <= lastPermutationMaxIndex) {
				// TODO initial check for bounds here
				checkLastPermutation = true;
			}

			if(checkLastPermutation) {
				// are we still within our designated range?
				// the next permutation must be lexicographically less than the first permutation
				// in the next block

				int i = 0;
				while (i < lastPermutation.length) {
					int value = permutations[i + ParallelPermutationRotationIteratorList.PADDING];
					if(value < lastPermutation[i]) {
						return resultIndex;
					} else if(value > lastPermutation[i]) {
						return -1;
					}
					i++;
				}
				// so all most be equal
				// we are at the exact last permutations
				return -1;
			}
		}

		return resultIndex;
	}

	public int nextWorkUnitPermutation(int[] permutations, int maxIndex) {
		while (maxIndex >= 0) {

			int current = permutations[ParallelPermutationRotationIteratorList.PADDING + maxIndex];

			int minIndex = -1;
			for (int i = ParallelPermutationRotationIteratorList.PADDING + maxIndex + 1; i < permutations.length; i++) {
				if(current < permutations[i] && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
					minIndex = i;
				}
			}

			if(minIndex == -1) {
				maxIndex--;

				continue;
			}

			// swap indexes
			permutations[ParallelPermutationRotationIteratorList.PADDING + maxIndex] = permutations[minIndex];
			permutations[minIndex] = current;

			Arrays.sort(permutations, ParallelPermutationRotationIteratorList.PADDING + maxIndex + 1, permutations.length);

			return maxIndex;
		}
		return -1;
	}

	public PermutationRotation get(int permutationIndex) {
		return matrix[permutations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]].getBoxes()[rotations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]];
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(getRotations(), getPermutations());
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		iterator.removePermutations(removed);
	}

	public long countPermutations() {
		return iterator.countPermutations();
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, ParallelPermutationRotationIteratorList.PADDING, rotations.length - ParallelPermutationRotationIteratorList.PADDING);
	}

	@Override
	public int length() {
		return permutations.length - ParallelPermutationRotationIteratorList.PADDING;
	}
}