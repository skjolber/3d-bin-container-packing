package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.StackableItem;

public abstract class AbstractStackableItemGroupPermutationRotationIterator extends AbstractStackableItemPermutationRotationIterator {

	protected List<IndexedStackableItemGroup> groups;
	
	public AbstractStackableItemGroupPermutationRotationIterator(IndexedStackableItem[] matrix, List<IndexedStackableItemGroup> groups) {
		super(matrix);
		this.groups = groups;
	}
	
	protected int getCount() {
		int index = 0;
		for (IndexedStackableItemGroup loadableItemGroup : groups) {
			index += loadableItemGroup.stackableItemsCount();
		}
		return index;
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

		for (IndexedStackableItemGroup loadableItemGroup : groups) {

			List<IndexedStackableItem> items = loadableItemGroup.getItems();
			
			int count = loadableItemGroup.stackableItemsCount();
			if(count == 0) {
				continue;
			}
			
			int maxCount = 0;
			for (StackableItem value : items) {
				if(value != null) {
					if(maxCount < value.getCount()) {
						maxCount = value.getCount();
					}
				}
			}
	
			if(maxCount > 1) {
				int[] factors = new int[maxCount];
				for (StackableItem value : items) {
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
			IndexedStackableItemGroup group = groups.get(i);
			
			group.removeEmpty();
			if(group.isEmpty()) {
				groups.remove(i);
				i--;
			}
		}
	}

	
	
}
