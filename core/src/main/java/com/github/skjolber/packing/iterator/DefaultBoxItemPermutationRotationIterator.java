package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

public class DefaultBoxItemPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractBoxItemIteratorBuilder<Builder> {

		public DefaultBoxItemPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			BoxItem[] matrix = toMatrix();

			return new DefaultBoxItemPermutationRotationIterator(matrix);
		}
	}

	public DefaultBoxItemPermutationRotationIterator(BoxItem[] matrix) {
		super(matrix);
		
		int count = 0;
		
		for (BoxItem loadableItem : matrix) {
			if(loadableItem != null) {
				count += loadableItem.getCount();
			}
		}
		
		this.minBoxVolume = new long[count];

		initiatePermutation(count);
	}
	
	public BoxStackValue getStackValue(int index) {
		return stackableItems[permutations[index]].getBox().getStackValue(rotations[index]);
	}

	public void removePermutations(int count) {
		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			BoxItem loadableItem = stackableItems[permutations[i]];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				stackableItems[i] = null;
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
		for (int j = 0; j < stackableItems.length; j++) {
			BoxItem value = stackableItems[j];
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

	public long getMinBoxVolume(int offset) {
		return minBoxVolume[offset];
	}

	/**
	 * Remove permutations, if present.
	 */
	
	public void removePermutations(List<Integer> removed) {

		int count = rotations.length;
		for (Integer i : removed) {
			BoxItem boxItem = stackableItems[i];
			if(boxItem != null) {
				boxItem.decrement();
				
				count--;
				
				if(boxItem.isEmpty()) {
					stackableItems[i] = null;
				}
			}
		}
		 
		initiatePermutation(count);
	}

	
	public int nextRotation() {
		// next rotation
		return nextRotation(rotations.length - 1);
	}
	
	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = maxIndex; i >= 0; i--) {
			if(rotations[i] < stackableItems[permutations[i]].getBox().getStackValues().length - 1) {
				rotations[i]++;

				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i;
			}
		}

		return -1;
	}

	
	@Override
	public int[] getPermutations() {
		int[] permutations = new int[this.permutations.length];
		System.arraycopy(this.permutations, 0, permutations, 0, permutations.length);
		return permutations;
	}

	protected void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
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

	public BoxItem getPermutation(int index) {
		return stackableItems[permutations[index]];
	}
	
	protected int[] getRotations() {
		return rotations;
	}
}
