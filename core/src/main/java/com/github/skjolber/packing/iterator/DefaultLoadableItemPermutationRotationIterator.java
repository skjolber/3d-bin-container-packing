package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;

public class DefaultLoadableItemPermutationRotationIterator extends AbstractLoadablePermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLoadableIteratorBuilder<Builder> {

		public DefaultLoadableItemPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			IndexedStackableItem[] matrix = toMatrix();

			return new DefaultLoadableItemPermutationRotationIterator(matrix);
		}

	}

	protected int[] rotations; // 2^n or 6^n

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minStackableVolume;

	public DefaultLoadableItemPermutationRotationIterator(IndexedStackableItem[] matrix) {
		super(matrix);
		
		int count = 0;
		
		for (IndexedStackableItem loadableItem : matrix) {
			if(loadableItem != null) {
				count += loadableItem.getCount();
			}
		}
		
		this.minStackableVolume = new long[count];

		initiatePermutation(count);
	}
	
	public StackValue getStackValue(int index) {
		return loadableItems[permutations[index]].getStackable().getStackValue(rotations[index]);
	}

	public void removePermutations(int count) {
		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			IndexedStackableItem loadableItem = loadableItems[permutations[i]];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				loadableItems[i] = null;
			}
		}
		
		initiatePermutation(rotations.length - count);
	}

	protected void initiatePermutation(int remainingCount) {
		this.rotations = new int[remainingCount];
		this.reset = new int[rotations.length];

		// need to be in ascending order for the algorithm to work
		int[] permutations = new int[rotations.length];
		
		int offset = 0;
		for (int j = 0; j < loadableItems.length; j++) {
			IndexedStackableItem value = loadableItems[j];
			if(value != null && !value.isEmpty()) {
				for (int k = 0; k < value.getCount(); k++) {
					permutations[offset] = j;
					offset++;
				}
			}
		}
		
		this.permutations = permutations;
		
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	protected void calculateMinStackableVolume(int offset) {
		StackValue last = loadableItems[permutations[permutations.length - 1]].getStackable().getStackValue(rotations[permutations.length - 1]);

		minStackableVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = loadableItems[permutations[i]].getStackable().getStackValue(rotations[i]).getVolume();

			if(volume < minStackableVolume[i + 1]) {
				minStackableVolume[i] = volume;
			} else {
				minStackableVolume[i] = minStackableVolume[i + 1];
			}
		}
	}

	public long getMinStackableVolume(int offset) {
		return minStackableVolume[offset];
	}
	
	protected long[] getMinStackableVolume() {
		return minStackableVolume;
	}
	

	/**
	 * Remove permutations, if present.
	 */
	
	public void removePermutations(List<Integer> removed) {
		
		 for (Integer i : removed) {
			IndexedStackableItem loadableItem = loadableItems[i];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				loadableItems[i] = null;
			}
		}
		 
		initiatePermutation(rotations.length - removed.size());
	}

	
	public int nextRotation() {
		// next rotation
		return nextRotation(rotations.length - 1);
	}
	
	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = maxIndex; i >= 0; i--) {
			if(rotations[i] < loadableItems[permutations[i]].getStackable().getStackValues().length - 1) {
				rotations[i]++;

				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i;
			}
		}

		return -1;
	}

	
	public int[] getPermutations() {
		return permutations;
	}

	protected void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public long countRotations() {
		long n = 1;
		for (int i = 0; i < permutations.length; i++) {
			IndexedStackableItem value = loadableItems[permutations[i]];
			if(Long.MAX_VALUE / value.getStackable().getStackValues().length <= n) {
				return -1L;
			}

			n = n * value.getStackable().getStackValues().length;
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
		for (IndexedStackableItem value : loadableItems) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (IndexedStackableItem value : loadableItems) {
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
	
	public int length() {
		return permutations.length;
	}
	
	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}

	public IndexedStackableItem getPermutation(int index) {
		return loadableItems[permutations[index]];
	}
	
	protected int[] getRotations() {
		return rotations;
	}
}
