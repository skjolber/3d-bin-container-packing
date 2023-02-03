package com.github.skjolber.packing.iterator;

import java.util.List;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * 
 */

public class ParallelPermutationRotationIteratorList {

	protected final static int PADDING = 16;

	protected final int[] frequencies;
	protected ParallelPermutationRotationIterator[] workUnits;

	public ParallelPermutationRotationIteratorList(PermutationStackableValue[] matrix, int parallelizationCount) {
		this.frequencies = new int[matrix.length];

		for (int i = 0; i < matrix.length; i++) {
			if(matrix[i] != null) {
				frequencies[i] = matrix[i].getCount();
			}
		}

		workUnits = new ParallelPermutationRotationIterator[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			workUnits[i] = new ParallelPermutationRotationIterator(matrix, this);
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}

		calculate();
	}

	public void removePermutations(List<Integer> removed) {
		for (Integer integer : removed) {
			frequencies[integer]--;
		}

		calculate();
	}

	private void calculate() {
		int count = getCount();

		int[] reset = new int[count];

		long countPermutations;
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			countPermutations = getPermutationCount(count);
		} else {
			countPermutations = getPermutationCountWithRepeatedItems(count, first);
		}

		if(countPermutations == -1L) {
			throw new IllegalArgumentException();
		}

		int[] copyOfFrequencies = new int[frequencies.length];
		for (int i = 0; i < workUnits.length; i++) {
			long rank = (countPermutations * i) / workUnits.length;

			rank++;

			// use more complex n-th lexographical permutation algorithm
			// which also handles zero frequencies
			System.arraycopy(frequencies, 0, copyOfFrequencies, 0, frequencies.length);
			int[] permutations = kthPermutation(copyOfFrequencies, count, countPermutations, rank);
			if(permutations.length < PADDING) {
				throw new IllegalStateException("Expected size >= " + PADDING + ", found " + permutations.length);
			}
			workUnits[i].setPermutations(permutations);
			workUnits[i].setRotations(new int[PADDING + reset.length]);
			workUnits[i].setReset(reset);
		}

		for (int i = 0; i < workUnits.length - 1; i++) {
			int[] nextWorkUnitPermutations = workUnits[i + 1].getPermutations();
			int[] lexiographicalLimit = new int[nextWorkUnitPermutations.length];

			System.arraycopy(nextWorkUnitPermutations, 0, lexiographicalLimit, 0, lexiographicalLimit.length);

			workUnits[i].setLastPermutation(lexiographicalLimit);
		}
	}

	private int getCount() {
		int count = 0;
		for (int f : frequencies) {
			count += f;
		}
		return count;
	}

	public long countPermutations() {
		return countPermutations(getCount());
	}

	long countPermutations(int count) {
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			return getPermutationCount(count);
		} else {
			return getPermutationCountWithRepeatedItems(count, first);
		}
	}

	private long getPermutationCount(int count) {
		long permutationCount = 1;
		for (int i = 0; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		return permutationCount;
	}

	private long getPermutationCountWithRepeatedItems(int count, int first) {
		long permutationCount = 1;
		// cancel out the first set of factors
		// 
		// For [3, 4] this would look like:
		//
		// 1 * 2 * 3 * 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3) (1 * 2 * 3 * 4)
		//
		// which is equal to
		//
		// 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3 * 4)
		//
		// above the line:
		for (int i = frequencies[first]; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		// below the line:
		for (int i = first + 1; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				for (int k = 1; k < frequencies[i]; k++) {
					permutationCount = permutationCount / (k + 1);
				}
			}
		}
		// future improvement: cancel out more
		return permutationCount;
	}

	private static int firstDuplicate(int[] frequencies) {
		for (int i = 0; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				return i;
			}
		}
		return -1;
	}

	static int[] kthPermutation(int[] frequencies, int elementCount, long permutationCount, long rank) {
		int[] result = new int[PADDING + elementCount];

		for (int i = 0; i < elementCount; i++) {
			for (int k = 0; k < frequencies.length; k++) {
				if(frequencies[k] == 0) {
					continue;
				}
				long suffixcount = permutationCount * frequencies[k] / (elementCount - i);
				if(rank <= suffixcount) {
					result[PADDING + i] = k;

					permutationCount = suffixcount;

					frequencies[k]--;
					break;
				}
				rank -= suffixcount;
			}
		}
		return result;
	}

	public ParallelPermutationRotationIterator[] getIterators() {
		return workUnits;
	}

	public ParallelPermutationRotationIterator getIterator(int i) {
		return workUnits[i];
	}
}
