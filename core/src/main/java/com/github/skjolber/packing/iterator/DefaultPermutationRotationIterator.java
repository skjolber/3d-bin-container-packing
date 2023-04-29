package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultPermutationRotationIterator extends AbstractPermutationRotationIterator implements PermutationRotationIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPermutationRotationIteratorBuilder<Builder> {

		public DefaultPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			PermutationStackableValue[] matrix = toMatrix();

			return new DefaultPermutationRotationIterator(matrix);
		}

	}

	protected int[] rotations; // 2^n or 6^n

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minStackableVolume;

	public DefaultPermutationRotationIterator(PermutationStackableValue[] matrix) {
		super(matrix);

		List<Integer> types = new ArrayList<>(matrix.length * 2);
		for (int j = 0; j < matrix.length; j++) {
			PermutationStackableValue value = matrix[j];
			if(value != null) {
				for (int k = 0; k < value.count; k++) {
					types.add(j);
				}
			}
		}

		this.reset = new int[types.size()];
		this.rotations = new int[types.size()];

		// permutations is a 'pointer' list
		// keep the the number of permutations tight;
		// identical boxes need not be interchanged
		permutations = new int[types.size()];

		for (int i = 0; i < permutations.length; i++) {
			permutations[i] = types.get(i);
		}

		this.minStackableVolume = new long[permutations.length];

		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	public void removePermutations(int count) {
		this.rotations = new int[rotations.length - count];
		this.reset = new int[rotations.length];

		// discard a number of items
		int newLength = permutations.length - count;

		int[] permutations = new int[this.permutations.length - count];
		System.arraycopy(this.permutations, count, permutations, 0, newLength);
		Arrays.sort(permutations); // ascending order to make the permutation logic work

		this.permutations = permutations;
	}

	private void calculateMinStackableVolume(int offset) {
		if(permutations.length > 0) {
			PermutationRotation last = get(permutations.length - 1);
	
			minStackableVolume[permutations.length - 1] = last.getValue().getVolume();
	
			for (int i = permutations.length - 2; i >= offset; i--) {
				long volume = get(i).getValue().getVolume();
	
				if(volume < minStackableVolume[i + 1]) {
					minStackableVolume[i] = volume;
				} else {
					minStackableVolume[i] = minStackableVolume[i + 1];
				}
			}
		}
	}

	public long getMinStackableVolume(int offset) {
		return minStackableVolume[offset];
	}

	/**
	 * Remove permutations, if present.
	 */
	@Override
	public void removePermutations(List<Integer> removed) {
		int left = permutations.length;
		permutations: for (Integer remove : removed) {
			for (int k = 0; k < permutations.length; k++) {
				if(remove == permutations[k]) {
					permutations[k] = -1; // mark as removed
					left--;
					continue permutations;
				}
			}
		}

		int[] effectivePermutations = new int[left];
		int destinationIndex = 0;

		for (int i = 0; i < permutations.length; i++) {
			if(permutations[i] != -1) {
				effectivePermutations[destinationIndex] = permutations[i];
				destinationIndex++;
			}
		}

		Arrays.sort(effectivePermutations); // ascending order to make the permutation logic work

		this.permutations = effectivePermutations;

		this.rotations = new int[effectivePermutations.length];
		this.reset = new int[effectivePermutations.length];

		calculateMinStackableVolume(0);
	}

	@Override
	public int nextRotation() {
		// next rotation
		return nextRotation(rotations.length - 1);
	}

	@Override
	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = maxIndex; i >= 0; i--) {
			if(rotations[i] < matrix[permutations[i]].getBoxes().length - 1) {
				rotations[i]++;

				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i;
			}
		}

		return -1;
	}

	@Override
	public int[] getPermutations() {
		return permutations;
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public long countRotations() {
		long n = 1;
		for (int i = 0; i < permutations.length; i++) {
			PermutationStackableValue value = matrix[permutations[i]];
			if(Long.MAX_VALUE / value.getBoxes().length <= n) {
				return -1L;
			}

			n = n * value.getBoxes().length;
		}
		return n;
	}

	/**
	 * Return number of permutations for boxes which fit within this container.
	 * 
	 * @return permutation count
	 */

	public long countPermutations() {
		// reduce permutations for boxes which are duplicated

		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume

		int maxCount = 0;
		for (PermutationStackableValue value : matrix) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (PermutationStackableValue value : matrix) {
				if(value != null) {
					for (int k = 0; k < value.getCount(); k++) {
						factors[k]++;
					}
				}
			}

			for (long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}

				n = n * (i + 1);

				for (int k = 1; k < maxCount; k++) {
					while (factors[k] > 0 && n % (k + 1) == 0) {
						n = n / (k + 1);

						factors[k]--;
					}
				}
			}

			for (int k = 1; k < maxCount; k++) {
				while (factors[k] > 0) {
					n = n / (k + 1);

					factors[k]--;
				}
			}
		} else {
			for (long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}

	@Override
	public PermutationRotation get(int index) {
		return matrix[permutations[index]].getBoxes()[rotations[index]];
	}

	public int nextPermutation(int maxIndex) {
		while (maxIndex >= 0) {

			int[] permutations = this.permutations;

			int current = permutations[maxIndex];

			// find the lexicographically next item to the right of the max index
			int minIndex = -1;
			for (int i = maxIndex + 1; i < permutations.length; i++) {
				if(permutations[i] > current && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
					minIndex = i;
				}
			}

			// if there is no such item, decrement and try again
			if(minIndex == -1) {
				// TODO search backwards?
				maxIndex--;

				continue;
			}

			// increment to the next lexigrapically item
			// and sort the items to the right of the max index
			permutations[maxIndex] = permutations[minIndex];
			permutations[minIndex] = current;

			Arrays.sort(permutations, maxIndex + 1, permutations.length);

			resetRotations();

			calculateMinStackableVolume(maxIndex);

			return maxIndex;
		}
		return -1;
	}

	@Override
	public int nextPermutation() {
		resetRotations();

		int[] permutations = this.permutations;

		// Find longest non-increasing suffix
		int i = permutations.length - 1;
		while (i > 0 && permutations[i - 1] >= permutations[i])
			i--;
		// Now i is the head index of the suffix

		// Are we at the last permutation already?
		if(i <= 0) {
			return -1;
		}

		// Let array[i - 1] be the pivot
		// Find rightmost element that exceeds the pivot
		int j = permutations.length - 1;
		while (permutations[j] <= permutations[i - 1])
			j--;
		// Now the value array[j] will become the new pivot
		// Assertion: j >= i

		int head = i - 1;

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

		calculateMinStackableVolume(head);

		// Successfully computed the next permutation
		return head;
	}

	@Override
	public int length() {
		return permutations.length;
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}

}
