package com.github.skjolber.packing.accelerator;

import java.util.Arrays;

/**
 * The permutations of the boxes of a {@linkplain FlatProblem}, as for {@code DefaultBoxItemPermutationRotationIterator}: lexicographic over the types,
 * where boxes of the same type are not told apart.
 * <p>
 * The methods are static and use primitive arrays only.
 */
public final class FlatPermutation {

	private FlatPermutation() {
	}

	/**
	 * Write the first permutation: the types in ascending order, each as many times as there are boxes of it.
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param permutation the array to write to, at the offset
	 */
	public static void first(int[] problem, int[] permutation, int offset) {
		int types = problem[FlatProblem.TYPES];
		int index = offset;
		for (int t = 0; t < types; t++) {
			for (int k = problem[FlatProblem.HEADER + t]; k > 0; k--) {
				permutation[index] = t;
				index++;
			}
		}
	}

	/**
	 * Step to the next permutation which differs at an index no higher than the max index. The boxes to the right of the changed index are then in
	 * ascending order. A port of {@code DefaultBoxItemPermutationRotationIterator.nextPermutation(int)}.
	 *
	 * @param permutation the permutation, at the offset
	 * @param n the number of boxes
	 * @param maxIndex the highest index to change
	 * @return the index which was changed, or -1 if there is no such permutation (the permutation is then unchanged)
	 */
	public static int next(int[] permutation, int offset, int n, int maxIndex) {
		// one exit and no continue: the OpenCL backend does not lower a return or continue in a loop correctly
		int changed = -1;
		int index = maxIndex;
		while (index >= 0) {
			int current = permutation[offset + index];

			// the smallest type to the right of the index which is larger than the current
			int minIndex = -1;
			for (int i = index + 1; i < n; i++) {
				int value = permutation[offset + i];
				if(value > current && (minIndex == -1 || value < permutation[offset + minIndex])) {
					minIndex = i;
				}
			}
			if(minIndex == -1) {
				index--;
			} else {
				permutation[offset + index] = permutation[offset + minIndex];
				permutation[offset + minIndex] = current;

				sort(permutation, offset + index + 1, offset + n);
				changed = index;
				index = -1;
			}
		}
		return changed;
	}

	/** Insertion sort of the range: the boxes to sort are few, and a sorted range stays cheap. */
	private static void sort(int[] values, int from, int to) {
		for (int i = from + 1; i < to; i++) {
			int value = values[i];
			int j = i - 1;
			while (j >= from && values[j] > value) {
				values[j + 1] = values[j];
				j--;
			}
			values[j + 1] = value;
		}
	}

	/**
	 * The first permutation of each distinct prefix of the given depth, in lexicographic order. The permutations of a prefix are those which start with
	 * it; each is the work of one item for {@link FlatSubtree}.
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param depth the length of the prefix
	 * @param maxItems the most prefixes to enumerate
	 * @return the n values of each prefix's first permutation after each other, or null if there are more than max items
	 */
	public static int[] prefixes(int[] problem, int depth, int maxItems) {
		int n = problem[FlatProblem.N];
		int[] current = new int[n];
		first(problem, current, 0);

		int[] result = new int[Math.min(maxItems, 64) * n];
		int count = 0;
		do {
			if(count == maxItems) {
				return null;
			}
			if((count + 1) * n > result.length) {
				result = Arrays.copyOf(result, (int)Math.min((long)maxItems * n, 2L * result.length));
			}
			System.arraycopy(current, 0, result, count * n, n);
			count++;
		} while (next(current, 0, n, depth - 1) != -1);
		return Arrays.copyOf(result, count * n);
	}

	/**
	 * Find the least depth with at least the given number of prefixes (or the greatest depth, where each prefix is one permutation).
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param minItems the number of prefixes wanted
	 * @param maxItems the most prefixes to hold
	 * @return the depth, which is at least 1 for a problem with boxes (and 0 for one without)
	 */
	public static int depth(int[] problem, int minItems, int maxItems) {
		int n = problem[FlatProblem.N];
		int best = 0;
		for (int depth = 1; depth <= n; depth++) {
			int[] prefixes = prefixes(problem, depth, maxItems);
			if(prefixes == null) {
				break;
			}
			best = depth;
			if(prefixes.length / n >= minItems) {
				break;
			}
		}
		return best;
	}
}
