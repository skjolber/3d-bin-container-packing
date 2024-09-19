package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;

public class DefaultStackableItemGroupPermutationRotationIterator extends AbstractStackableItemGroupPermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractStackableItemGroupIteratorBuilder<Builder> {

		public DefaultStackableItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<IndexedStackableItemGroup> groups = toMatrix();
			
			List<StackableItem> matrix = new ArrayList<>();
			for (IndexedStackableItemGroup loadableItemGroup : groups) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			return new DefaultStackableItemGroupPermutationRotationIterator(groups, matrix.toArray(new IndexedStackableItem[matrix.size()]));
		}

	}

	protected int[] rotations; // 2^n or 6^n
	protected int[] reset;

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minStackableVolume;
	
	public DefaultStackableItemGroupPermutationRotationIterator(List<IndexedStackableItemGroup> groups, IndexedStackableItem[] matrix) {
		super(matrix, groups);
		
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
		return stackableItems[permutations[index]].getStackable().getStackValue(rotations[index]);
	}

	public void removePermutations(int count) {
		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			IndexedStackableItem item = stackableItems[permutations[i]];
			
			item.decrement();
			
			if(item.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		
		for(int i = 0; i < groups.size(); i++) {
			IndexedStackableItemGroup group = groups.get(i);
			
			group.removeEmpty();
			
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
			} else {
				break;
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
			IndexedStackableItem value = stackableItems[j];
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
		IndexedStackableItem value = stackableItems[permutations[permutations.length - 1]];

		minStackableVolume[permutations.length - 1] = value.getStackable().getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = stackableItems[permutations[i]].getStackable().getStackValue(rotations[i]).getVolume();

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
		
		super.removePermutations(removed);
		
		initiatePermutation(rotations.length - removed.size());
	}

	
	public int nextRotation() {
		// next rotation
		return nextRotation(rotations.length - 1);
	}
	
	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = maxIndex; i >= 0; i--) {
			if(rotations[i] < stackableItems[permutations[i]].getStackable().getStackValues().length - 1) {
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

	public long countRotations() {
		long n = 1;
		for (int i = 0; i < permutations.length; i++) {
			IndexedStackableItem value = stackableItems[permutations[i]];
			if(Long.MAX_VALUE / value.getStackable().getStackValues().length <= n) {
				return -1L;
			}

			n = n * value.getStackable().getStackValues().length;
		}
		return n;
	}

	public int nextPermutation(int maxIndex) {
		int[] permutations = this.permutations;

		int limit = permutations.length;

		for(int g = groups.size() - 1; g >= 0; g--) {
			IndexedStackableItemGroup loadableItemGroup = groups.get(g);

			// Find longest non-increasing suffix
			int startIndex = limit - loadableItemGroup.stackableItemsCount();

			if(startIndex <= maxIndex && maxIndex < limit) {
				while (maxIndex >= startIndex) {

					int current = permutations[maxIndex];

					// find the lexicographically next item to the right of the max index
					int minIndex = -1;
					for (int i = maxIndex + 1; i < limit; i++) {
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

					// TODO: better to recreate?
					Arrays.sort(permutations, maxIndex + 1, limit);

					resetRotations();

					calculateMinStackableVolume(maxIndex);

					return maxIndex;
				}				
			}
			// reset current group
			// TODO system arraycopy?
			int i = startIndex;
			
			for (StackableItem loadableItem : loadableItemGroup.getItems()) {
				IndexedStackableItem indexedStackableItem = (IndexedStackableItem)loadableItem;
				for(int k = 0; k < indexedStackableItem.getCount(); k++) {
					permutations[i] = indexedStackableItem.getIndex();
							
					i++;
				}
			}

			// skip to next group
			limit = startIndex;
		}
		
		return -1;
	}

	public int nextPermutation() {
		resetRotations();

		int[] permutations = this.permutations;

		int endIndex = permutations.length - 1;

		for(int g = groups.size() - 1; g >= 0; g--) {
			IndexedStackableItemGroup loadableItemGroup = groups.get(g);

			// Find longest non-increasing suffix
			int i = endIndex;
			int startIndex = endIndex - loadableItemGroup.stackableItemsCount() + 1;

			while (i > startIndex && permutations[i - 1] >= permutations[i])
				i--;
			// Now i is the head index of the suffix

			// Are we at the last permutation already?
			if(i <= startIndex) {
				// reset current group
				// TODO system arraycopy?
				i = startIndex;
				
				for (StackableItem loadableItem : loadableItemGroup.getItems()) {
					IndexedStackableItem indexedStackableItem = (IndexedStackableItem)loadableItem;
					for(int k = 0; k < indexedStackableItem.getCount(); k++) {
						permutations[i] = indexedStackableItem.getIndex();
								
						i++;
					}
				}

				// skip to next group
				endIndex = startIndex - 1;
				
				continue;
			}

			// Let array[i - 1] be the pivot
			// Find rightmost element that exceeds the pivot
			int j = endIndex;
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
			j = endIndex;
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
		
		return -1;
	}
	
	public int length() {
		return permutations.length;
	}
	
	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}

	public IndexedStackableItem getPermutation(int index) {
		return stackableItems[permutations[index]];
	}
	
	protected int[] getRotations() {
		return rotations;
	}
	
	public List<IndexedStackableItemGroup> getGroups() {
		return groups;
	}

}
