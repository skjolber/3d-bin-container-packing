package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

public class ParallelBoxItemGroupPermutationRotationIterator extends AbstractBoxItemGroupsPermutationRotationIterator {
	
	protected final int PADDING = 16;
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends AbstractBoxItemGroupIteratorBuilder<Builder> {

		public ParallelBoxItemGroupPermutationRotationIterator build() {
			List<BoxItemGroup> included = new ArrayList<>(boxItemGroups.size());
			List<BoxItemGroup> excluded = new ArrayList<>(boxItemGroups.size());
			
			// box item and box item groups indexes are unique and static
			
			int offset = 0;
			for (int i = 0; i < boxItemGroups.size(); i++) {
				BoxItemGroup group = boxItemGroups.get(i);
				if(fitsInside(group)) {
					List<BoxItem> loadableItems = new ArrayList<>(group.size());
					for (int k = 0; k < group.size(); k++) {
						BoxItem item = group.get(k);
	
						Box box = item.getBox();
						
						List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
						Box boxClone = new Box(box, boundRotations);
						
						loadableItems.add(new BoxItem(boxClone, item.getCount(), offset));
						
						offset++;
					}
					included.add(new BoxItemGroup(group.getId(), loadableItems, i));
				} else {
					excluded.add(group);
					
					offset += group.size();
				}
			}

			BoxItemGroup[] groupIndex = new BoxItemGroup[boxItemGroups.size()];
			BoxItem[] boxIndex = new BoxItem[offset];
			
			for (BoxItemGroup loadableItemGroup : included) {
				groupIndex[loadableItemGroup.getIndex()] = loadableItemGroup;
				for (int k = 0; k < loadableItemGroup.size(); k++) {
					BoxItem item = loadableItemGroup.get(k);
					boxIndex[item.getIndex()] = item;
				}
			}
			
			ParallelBoxItemGroupPermutationRotationIterator result = new ParallelBoxItemGroupPermutationRotationIterator(groupIndex, boxIndex, excluded);
			
			result.initiatePermutations();
			
			return result;
		}
	}

	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;

	private int[] lastPermutation;
	private int lastPermutationMaxIndex = -1;
	private boolean seenLastPermutationMaxIndex = false;

	private List<Integer> excluded;


	public ParallelBoxItemGroupPermutationRotationIterator(BoxItemGroup[] groupsMatrix, BoxItem[] boxMatrix, List<BoxItemGroup> excluded) {
		super(groupsMatrix, boxMatrix, excluded);
	}	
	
	public long preventOptmisation() {
		return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
	}

	public void setReset(int[] reset) {
		this.reset = reset;
	}

	public int[] getPermutations() {
		int[] result = new int[permutations.length - PADDING];
		System.arraycopy(permutations, PADDING, result, 0, result.length);
		return result;
	}

	public void setPermutations(int[] permutations) {
		this.permutations = permutations;
	}

	public void initMinStackableVolume() {
		this.minBoxVolume = new long[permutations.length]; // i.e. with padding

		calculateMinStackableVolume(0);
	}

	public void calculateMinStackableVolume(int offset) {
		super.calculateMinStackableVolume(offset + PADDING);
	}

	public long getMinBoxVolume(int offset) {
		return super.getMinBoxVolume(PADDING + offset);
	}

	public int[] getRotations() {
		int[] result = new int[rotations.length - PADDING];
		System.arraycopy(rotations, PADDING, result, 0, result.length);
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
		for (int k = PADDING; k < lastPermutation.length; k++) {
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
		return nextRotation(rotations.length - 1 - PADDING);
	}

	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = PADDING + maxIndex; i >= PADDING; i--) {
			if(rotations[i] < stackableItems[permutations[i]].getBox().getStackValues().length - 1) {
				rotations[i]++;

				// reset all following counters
				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i - PADDING;
			}
		}

		return -1;
	}

	protected int nextPermutationImpl() {
		// https://www.baeldung.com/cs/array-generate-all-permutations#permutations-in-lexicographic-order
		
		int[] permutations = this.permutations;

		int endIndex = permutations.length - 1;

		for(int g = groupsMatrix.length - 1; g >= 0; g--) {
			if(groupsMatrix[g] == null) {
				continue;
			}
			BoxItemGroup loadableItemGroup = groupsMatrix[g];
			
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
	
			int head = i - 1 - PADDING;
	
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
				
				int i = PADDING;
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

		for(int g = groupsMatrix.length - 1; g >= 0; g--) {
			if(groupsMatrix[g] == null) {
				continue;
			}
			BoxItemGroup loadableItemGroup = groupsMatrix[g];

			// Find longest non-increasing suffix
			int startIndex = limit - loadableItemGroup.getBoxCount();

			if(startIndex <= PADDING + maxIndex && PADDING + maxIndex < limit) {
				
				while (PADDING  + maxIndex >= startIndex) {
		
					int current = permutations[PADDING + maxIndex];
		
					int minIndex = -1;
					for (int i = PADDING + maxIndex + 1; i < permutations.length; i++) {
						if(current < permutations[i] && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
							minIndex = i;
						}
					}
		
					if(minIndex == -1) {
						maxIndex--;
		
						continue;
					}

					// swap indexes
					permutations[PADDING + maxIndex] = permutations[minIndex];
					permutations[minIndex] = current;
		
					Arrays.sort(permutations, PADDING + maxIndex + 1, permutations.length);
		
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
		return stackableItems[permutations[PADDING + permutationIndex]].getBox().getStackValue(rotations[PADDING + permutationIndex]);
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(getRotations(), getPermutations());
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, PADDING, rotations.length - PADDING);
	}

	@Override
	public int length() {
		return permutations.length - PADDING;
	}
	
	@Override
	public void removePermutations(int count) {
		List<Integer> removed = new ArrayList<>(permutations.length);
		
		for(int i = 0; i < count; i++) {
			removed.add(permutations[PADDING + i]);
		}
		
		removePermutations(removed);
	}

	@Override
	public BoxStackValue getStackValue(int index) {
		return super.getStackValue(PADDING + index);
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
		int[] permutations = new int[PADDING + count];
		
		int offset = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			BoxItem value = stackableItems[j];
			if(value != null && !value.isEmpty()) {
				for (int k = 0; k < value.getCount(); k++) {
					permutations[PADDING + offset] = j;
					offset++;
				}
			}
		}
		
		initiatePermutation(permutations);
		
		int[] lastPermutation = new int[PADDING + count];
		
		count = 0;
		for(int g = 0; g < groupsMatrix.length; g++) {
			if(groupsMatrix[g] == null) {
				continue;
			}
			
			BoxItemGroup group = groupsMatrix[g];
			
			int boxCount = group.getBoxCount();
			
			for(int i = 0; i < boxCount; i++) {
				lastPermutation[PADDING + count + i] = permutations[PADDING + count + boxCount - 1 - i];
			}
			count += boxCount;
		}		
		
		setLastPermutation(lastPermutation);
		
		// include the last permutation
		lastPermutationMaxIndex = -1;
	}

	protected void initiatePermutation(int[] permutations) {
		this.permutations = permutations;		
		this.rotations = new int[permutations.length];
		this.reset = new int[rotations.length];
		
		if(permutations.length > PADDING) {
			initMinStackableVolume();
		}
		
		seenLastPermutationMaxIndex = true;
	}

	public List<Integer> getExcluded() {
		return excluded;
	}
	
}