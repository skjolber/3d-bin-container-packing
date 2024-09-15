package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.StackableItemGroup;

public class ParallelStackableItemGroupPermutationRotationIterator extends AbstractStackableItemGroupPermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends AbstractStackableItemGroupIteratorBuilder<Builder> {

		public ParallelStackableItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<StackableItemGroup> groups = toMatrix();
			
			List<StackableItem> matrix = new ArrayList<>();
			for (StackableItemGroup loadableItemGroup : groups) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			ParallelStackableItemGroupPermutationRotationIterator result = new ParallelStackableItemGroupPermutationRotationIterator(matrix.toArray(new IndexedStackableItem[matrix.size()]), groups);
			
			result.initiatePermutations();
			
			return result;
		}
	}

	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;

	private int[] permutations;
	private int[] rotations;

	private int[] lastPermutation;
	private int lastPermutationMaxIndex = -1;
	private boolean checkLastPermutation = false;

	// minimum volume from index i and above
	protected long[] minStackableVolume;

	public ParallelStackableItemGroupPermutationRotationIterator(IndexedStackableItem[] matrix, List<StackableItemGroup> groups) {
		super(matrix, groups);
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
	
	public void setGroups(List<StackableItemGroup> groups) {
		this.groups = groups;
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
			IndexedStackableItem value = stackableItems[permutations[permutations.length - 1]];
	
			minStackableVolume[permutations.length - 1] = value.getStackable().getVolume();
			for (int i = permutations.length - 2; i >= offset + ParallelPermutationRotationIteratorList.PADDING; i--) {
				long volume = stackableItems[permutations[i]].getStackable().getVolume();
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

	public void setLastPermutation(int[] lastPermutation) { // array without padding
		this.lastPermutation = lastPermutation;

		this.checkLastPermutation = false;

		// find the first item that differs, so that we do not have to
		// compare items for each iteration (to detect whether we have done enough work)
		for (int k = ParallelPermutationRotationIteratorList.PADDING; k < ParallelPermutationRotationIteratorList.PADDING + lastPermutation.length; k++) {
			if(permutations[k] != lastPermutation[k]) {
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
			if(rotations[i] < stackableItems[permutations[i]].getStackable().getStackValues().length - 1) {
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
		
		int[] permutations = this.permutations;

		int endIndex = permutations.length - 1;

		for(int g = groups.size() - 1; g >= 0; g--) {
		
			StackableItemGroup loadableItemGroup = groups.get(g);
			
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
	
			int head = i - 1 - ParallelPermutationRotationIteratorList.PADDING;
	
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
	
			// Successfully computed the next permutation
			return head;
		}
		return -1;

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

		int resultIndex = nextPermutationImpl(maxIndex);

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

				int i = ParallelPermutationRotationIteratorList.PADDING;
				while (i < lastPermutation.length) {
					int value = permutations[i];
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

	public int nextPermutationImpl(int maxIndex) {
		int limit = permutations.length;

		for(int g = groups.size() - 1; g >= 0; g--) {
			StackableItemGroup loadableItemGroup = groups.get(g);

			// Find longest non-increasing suffix
			int startIndex = limit - loadableItemGroup.stackableItemsCount();

			if(startIndex <= maxIndex && maxIndex < limit) {
				
				while (maxIndex >= startIndex) {
		
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

	public StackValue get(int permutationIndex) {
		return stackableItems[permutations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]].getStackable().getStackValue(rotations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]);
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(getRotations(), getPermutations());
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, ParallelPermutationRotationIteratorList.PADDING, rotations.length - ParallelPermutationRotationIteratorList.PADDING);
	}

	@Override
	public int length() {
		return permutations.length - ParallelPermutationRotationIteratorList.PADDING;
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

	/**
	 * Remove permutations, if present.
	 */
	
	@Override
	public void removePermutations(List<Integer> removed) {
		
		 for (Integer i : removed) {
			IndexedStackableItem loadableItem = stackableItems[i];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		 
		// go through all groups and clean up
		for(int i = 0; i < groups.size(); i++) {
			StackableItemGroup group = groups.get(i);
			
			group.removeEmpty();
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
			}
		}
	}
	
	@Override
	public void removePermutations(int count) {

		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			IndexedStackableItem item = stackableItems[permutations[i]];
			
			item.decrement();
			
			if(item.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		
		// go through all groups and clean up
		for(int i = 0; i < groups.size(); i++) {
			StackableItemGroup group = groups.get(i);
			
			group.removeEmpty();
			
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
			} else {
				break;
			}
		}
	}

	@Override
	public StackValue getStackValue(int index) {
		return stackableItems[permutations[ParallelPermutationRotationIteratorList.PADDING + index]].getStackable().getStackValue(rotations[ParallelPermutationRotationIteratorList.PADDING + index]);
	}

	protected void initiatePermutations() {
		int count = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			IndexedStackableItem value = stackableItems[j];
			if(value != null && !value.isEmpty()) {
				count += value.getCount();
			}
		}
		
		// need to be in ascending order for the algorithm to work
		int[] permutations = new int[ParallelPermutationRotationIteratorList.PADDING + count];
		
		int offset = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			IndexedStackableItem value = stackableItems[j];
			if(value != null && !value.isEmpty()) {
				for (int k = 0; k < value.getCount(); k++) {
					permutations[ParallelPermutationRotationIteratorList.PADDING + offset] = j;
					offset++;
				}
			}
		}
		
		initiatePermutation(permutations);
		
		int[] lastPermutation = new int[ParallelPermutationRotationIteratorList.PADDING + count];
		
		count = 0;
		for(int g = 0; g < groups.size(); g++) {
			StackableItemGroup group = groups.get(g);
			
			int stackableItemsCount = group.stackableItemsCount();
			
			for(int i = 0; i < stackableItemsCount; i++) {
				lastPermutation[ParallelPermutationRotationIteratorList.PADDING + count + i] = permutations[ParallelPermutationRotationIteratorList.PADDING + count + stackableItemsCount - 1 - i];
			}
			count += stackableItemsCount;
		}		
		
		setLastPermutation(lastPermutation);
		
		// include the last permutation
		lastPermutationMaxIndex = -1;
	}

	protected void initiatePermutation(int[] permutations) {
		this.permutations = permutations;		
		this.rotations = new int[permutations.length];
		this.reset = new int[rotations.length];
		
		if(permutations.length > ParallelPermutationRotationIteratorList.PADDING) {
			initMinStackableVolume();
		}
		
		checkLastPermutation = true;
	}


}