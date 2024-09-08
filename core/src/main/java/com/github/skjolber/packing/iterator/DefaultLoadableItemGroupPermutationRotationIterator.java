package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.packager.LoadableItem;
import com.github.skjolber.packing.api.packager.LoadableItemGroup;

public class DefaultLoadableItemGroupPermutationRotationIterator extends AbstractLoadablePermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLoadableItemGroupIteratorBuilder<Builder> {

		public DefaultLoadableItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<LoadableItemGroup> groups = toMatrix();
			
			List<LoadableItem> matrix = new ArrayList<>();
			for (LoadableItemGroup loadableItemGroup : groups) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			return new DefaultLoadableItemGroupPermutationRotationIterator(groups, matrix.toArray(new LoadableItem[matrix.size()]));
		}

	}

	protected int[] rotations; // 2^n or 6^n

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minStackableVolume;
	
	protected List<LoadableItemGroup> groups;

	public DefaultLoadableItemGroupPermutationRotationIterator(List<LoadableItemGroup> groups, LoadableItem[] matrix) {
		super(matrix);
		
		this.groups = groups;
		
		int count = 0;
		
		for (LoadableItem loadableItem : matrix) {
			if(loadableItem != null) {
				count += loadableItem.getCount();
			}
		}
		
		this.minStackableVolume = new long[count];

		initiatePermutation(count);
	}
	
	public StackValue getStackValue(int index) {
		return loadableItems[permutations[index]].getLoadable().getStackValue(rotations[index]);
	}

	public void removePermutations(int count) {
		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			LoadableItem loadableItem = loadableItems[permutations[i]];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				loadableItems[i] = null;
			}
		}
		
		for(int i = 0; i < groups.size(); i++) {
			LoadableItemGroup group = groups.get(i);
			
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
		for (int j = 0; j < loadableItems.length; j++) {
			LoadableItem value = loadableItems[j];
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
		StackValue last = loadableItems[permutations[permutations.length - 1]].getLoadable().getStackValue(rotations[permutations.length - 1]);

		minStackableVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = loadableItems[permutations[i]].getLoadable().getStackValue(rotations[i]).getVolume();

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
			LoadableItem loadableItem = loadableItems[i];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				loadableItems[i] = null;
			}
		}
		 
		// go through all groups and clean up
		for(int i = 0; i < groups.size(); i++) {
			LoadableItemGroup group = groups.get(i);
			
			group.removeEmpty();
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
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
			if(rotations[i] < loadableItems[permutations[i]].getLoadable().getValues().size() - 1) {
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
			LoadableItem value = loadableItems[permutations[i]];
			if(Long.MAX_VALUE / value.getLoadable().getValues().size() <= n) {
				return -1L;
			}

			n = n * value.getLoadable().getValues().size();
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
		long n = 1;

		for (LoadableItemGroup loadableItemGroup : groups) {

			List<LoadableItem> items = loadableItemGroup.getItems();
			
			int count = loadableItemGroup.loadableItemsCount();
			
			int maxCount = 0;
			for (LoadableItem value : items) {
				if(value != null) {
					if(maxCount < value.getCount()) {
						maxCount = value.getCount();
					}
				}
			}
	
			if(maxCount > 1) {
				int[] factors = new int[maxCount];
				for (LoadableItem value : items) {
					if(value != null) {
						for (int k = 0; k < value.getCount(); k++) {
							factors[k]++;
						}
					}
				}
	
				for (long i = 0; i < count; i++) {
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
				for (long i = 0; i < count; i++) {
					if(Long.MAX_VALUE / (i + 1) <= n) {
						return -1L;
					}
					n = n * (i + 1);
				}
			}
		}
		return n;
	}

	public int nextPermutation(int maxIndex) {
		
		int limit = permutations.length;

		for(int g = groups.size() - 1; g >= 0; g--) {
			LoadableItemGroup loadableItemGroup = groups.get(g);

			// Find longest non-increasing suffix
			int startIndex = limit - loadableItemGroup.loadableItemsCount();

			if(startIndex <= maxIndex && maxIndex < limit) {
				while (maxIndex >= startIndex) {

					int[] permutations = this.permutations;

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

					Arrays.sort(permutations, maxIndex + 1, limit);

					resetRotations();

					calculateMinStackableVolume(maxIndex);

					return maxIndex;
				}				
			}
			// reset current group
			// TODO system arraycopy?
			int i = startIndex;
			
			for (LoadableItem loadableItem : loadableItemGroup.getItems()) {
				for(int k = 0; k < loadableItem.getCount(); k++) {
					permutations[i] = loadableItem.getIndex();
							
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

		int endIndex = permutations.length - 1;

		for(int g = groups.size() - 1; g >= 0; g--) {
		
			LoadableItemGroup loadableItemGroup = groups.get(g);

			int[] permutations = this.permutations;

			// Find longest non-increasing suffix
			int i = endIndex;
			int startIndex = endIndex - loadableItemGroup.loadableItemsCount() + 1;

			while (i > startIndex && permutations[i - 1] >= permutations[i])
				i--;
			// Now i is the head index of the suffix

			// Are we at the last permutation already?
			if(i <= startIndex) {
				// reset current group
				// TODO system arraycopy?
				i = startIndex;
				
				for (LoadableItem loadableItem : loadableItemGroup.getItems()) {
					for(int k = 0; k < loadableItem.getCount(); k++) {
						permutations[i] = loadableItem.getIndex();
								
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

			g++;
			
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

	public LoadableItem getPermutation(int index) {
		return loadableItems[permutations[index]];
	}
	
	protected int[] getRotations() {
		return rotations;
	}
}
