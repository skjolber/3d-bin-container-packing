package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

public class ParallelBoxItemGroupPermutationRotationIterator extends AbstractBoxItemGroupPermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends AbstractBoxItemGroupIteratorBuilder<Builder> {

		public ParallelBoxItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<BoxItemGroup> groups = toMatrix();
			
			List<BoxItem> matrix = new ArrayList<>();
			for (BoxItemGroup loadableItemGroup : groups) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			ParallelBoxItemGroupPermutationRotationIterator result = new ParallelBoxItemGroupPermutationRotationIterator(matrix.toArray(new BoxItem[matrix.size()]), groups);
			
			result.initiatePermutations();
			
			return result;
		}
	}

	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;

	private int[] lastPermutation;
	private int lastPermutationMaxIndex = -1;
	private boolean seenLastPermutationMaxIndex = false;

	public ParallelBoxItemGroupPermutationRotationIterator(BoxItem[] matrix, List<BoxItemGroup> groups) {
		super(matrix, groups);
	}

	public long preventOptmisation() {
		return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
	}

	public void setReset(int[] reset) {
		this.reset = reset;
	}

	public int[] getPermutations() {
		int[] result = new int[permutations.length - ParallelPermutationRotationIteratorList.PADDING];
		System.arraycopy(permutations, ParallelPermutationRotationIteratorList.PADDING, result, 0, result.length);
		return result;
	}
	
	public void setGroups(List<BoxItemGroup> groups) {
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
		super.calculateMinStackableVolume(offset + ParallelPermutationRotationIteratorList.PADDING);
	}

	public long getMinStackableVolume(int offset) {
		return super.getMinStackableVolume(ParallelPermutationRotationIteratorList.PADDING + offset);
	}

	public int[] getRotations() {
		int[] result = new int[rotations.length - ParallelPermutationRotationIteratorList.PADDING];
		System.arraycopy(rotations, ParallelPermutationRotationIteratorList.PADDING, result, 0, result.length);
		return result;
	}

	public void setRotations(int[] rotations) {
		this.rotations = rotations;
	}

	public void setLastPermutation(int[] lastPermutation) { // array without padding
		this.lastPermutation = lastPermutation;

		this.seenLastPermutationMaxIndex = false;

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
			if(rotations[i] < stackableItems[permutations[i]].getBox().getStackValues().length - 1) {
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
		
			BoxItemGroup loadableItemGroup = groups.get(g);
			
			// Find longest non-increasing suffix
	
			int i = endIndex;
			int startIndex = endIndex - loadableItemGroup.getBoxCount() + 1;

			while (i > startIndex && permutations[i - 1] >= permutations[i])
				i--;
			// Now i is the head index of the suffix
	
			// Are we at the last permutation already?
			if(i <= startIndex) {
				
				// reset current group
				// TODO system arraycopy?
				i = startIndex;
				
				for (BoxItem loadableItem : loadableItemGroup.getItems()) {
					BoxItem indexedStackableItem = (BoxItem)loadableItem;
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
				seenLastPermutationMaxIndex = true;
			}

			if(seenLastPermutationMaxIndex) {
				// are we still within our designated range?
				// the next permutation must be lexicographically less than the first permutation
				// in the next block

				// TODO is there a faster way to do this? 
				
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
			BoxItemGroup loadableItemGroup = groups.get(g);

			// Find longest non-increasing suffix
			int startIndex = limit - loadableItemGroup.getBoxCount();

			if(startIndex <= ParallelPermutationRotationIteratorList.PADDING + maxIndex && ParallelPermutationRotationIteratorList.PADDING + maxIndex < limit) {
				
				while (ParallelPermutationRotationIteratorList.PADDING  + maxIndex >= startIndex) {
		
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
			
			for (BoxItem loadableItem : loadableItemGroup.getItems()) {
				BoxItem indexedStackableItem = (BoxItem)loadableItem;
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

	public BoxStackValue get(int permutationIndex) {
		return stackableItems[permutations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]].getBox().getStackValue(rotations[ParallelPermutationRotationIteratorList.PADDING + permutationIndex]);
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

	/**
	 * Remove permutations, if present.
	 */
	
	@Override
	public void removePermutations(List<Integer> removed) {
		 for (Integer i : removed) {
			BoxItem item = stackableItems[i];
			
			item.decrement();
			
			if(item.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		 
		// go through all groups and clean up
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			
			group.removeEmpty();
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
			}
		}
	}
	
	@Override
	public void removePermutations(int count) {
		List<Integer> removed = new ArrayList<>(permutations.length);
		
		for(int i = 0; i < count; i++) {
			removed.add(permutations[ParallelPermutationRotationIteratorList.PADDING + i]);
		}
		
		removePermutations(removed);
	}

	@Override
	public BoxStackValue getStackValue(int index) {
		return super.getStackValue(ParallelPermutationRotationIteratorList.PADDING + index);
	}

	protected void initiatePermutations() {
		int count = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			BoxItem value = stackableItems[j];
			if(value != null && !value.isEmpty()) {
				count += value.getCount();
			}
		}
		
		// need to be in ascending order for the algorithm to work
		int[] permutations = new int[ParallelPermutationRotationIteratorList.PADDING + count];
		
		int offset = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			BoxItem value = stackableItems[j];
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
			BoxItemGroup group = groups.get(g);
			
			int stackableItemsCount = group.getBoxCount();
			
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
		
		seenLastPermutationMaxIndex = true;
	}

	public List<BoxItemGroup> getGroups() {
		return groups;
	}

}