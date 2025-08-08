package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public abstract class AbstractBoxItemGroupsPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator implements BoxItemGroupPermutationRotationIterator {

	protected BoxItemGroup[] groupsMatrix;

	protected List<BoxItemGroup> excludedBoxItemGroups;

	public AbstractBoxItemGroupsPermutationRotationIterator(BoxItemGroup[] groupsMatrix, BoxItem[] boxMatrix, List<BoxItemGroup> excluded) {
		super(boxMatrix);
		this.groupsMatrix = groupsMatrix;
		this.excludedBoxItemGroups = excluded;
	}
	
	protected int getCount() {
		int index = 0;
		for (BoxItemGroup loadableItemGroup : groupsMatrix) {
			if(loadableItemGroup == null) {
				continue;
			}
			index += loadableItemGroup.getBoxCount();
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

		for (BoxItemGroup loadableItemGroup : groupsMatrix) {
			if(loadableItemGroup == null) {
				continue;
			}

			List<BoxItem> items = loadableItemGroup.getItems();
			
			int count = loadableItemGroup.getBoxCount();
			
			if(count == 0) {
				continue;
			}
			
			int maxCount = 0;
			for (BoxItem value : items) {
				if(value != null) {
					if(maxCount < value.getCount()) {
						maxCount = value.getCount();
					}
				}
			}
	
			if(maxCount > 1) {
				int[] factors = new int[maxCount];
				for (BoxItem value : items) {
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

	public int removeGroups(List<Integer> removed) {
		int count = 0;
		for (Integer i : removed) {
			BoxItemGroup boxItemGroup = groupsMatrix[i];
			for (BoxItem boxItem : boxItemGroup.getItems()) {
				count += boxItem.getCount();

				boxItem.setCount(0);
				stackableItems[boxItem.getIndex()] = null;
			}
			groupsMatrix[i] = null;
		}
		return count;
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		 for (Integer i : removed) {
			BoxItem boxItem = stackableItems[i];
			
			boxItem.decrement();
			
			if(boxItem.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		
		for(int i = 0; i < groupsMatrix.length; i++) {
			BoxItemGroup group = groupsMatrix[i];
			if(group == null) {
				continue;
			}
			group.removeEmpty();
			if(group.isEmpty()) {
				groupsMatrix[i] = null;
			}
		}
		
	}
	
	public BoxItemGroup[] getBoxItemGroups() {
		return groupsMatrix;
	}
	
	public List<BoxItemGroup> getExcludedBoxItemGroups() {
		return excludedBoxItemGroups;
	}
	
}
