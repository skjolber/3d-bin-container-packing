package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

public abstract class AbstractBoxItemGroupsPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator implements BoxItemGroupPermutationRotationIterator {

	protected RemainingBoxItemGroup[] groupsMatrix;
	protected List<RemainingBoxItemGroup> excludedBoxItemGroups;

	public AbstractBoxItemGroupsPermutationRotationIterator(RemainingBoxItemGroup[] groupsMatrix, RemainingBoxItem[] boxMatrix, BoxStackValue[][] stackValues, List<RemainingBoxItemGroup> excluded) {
		super(boxMatrix, stackValues);
		this.groupsMatrix = groupsMatrix;
		this.excludedBoxItemGroups = excluded;
	}

	protected record GroupState(RemainingBoxItemGroup[] groups, RemainingBoxItem[] boxes) {}

	protected static GroupState copyGroupState(AbstractBoxItemGroupsPermutationRotationIterator source) {
		RemainingBoxItem[] boxes = copyBoxItems(source.stackableItems);
		RemainingBoxItemGroup[] groups = new RemainingBoxItemGroup[source.groupsMatrix.length];
		for(int i = 0; i < groups.length; i++) {
			RemainingBoxItemGroup group = source.groupsMatrix[i];
			if(group != null) {
				List<RemainingBoxItem> items = new ArrayList<>(group.size());
				for(RemainingBoxItem item : group.getItems()) {
					items.add(boxes[item.getLocalIndex()]);
				}
				groups[i] = new RemainingBoxItemGroup(group.getBoxItemGroup(), items, group.getIndex());
			}
		}
		return new GroupState(groups, boxes);
	}
	
	protected int getBoxCount() {
		int count = 0;
		for (RemainingBoxItemGroup group : groupsMatrix) {
			if(group == null) {
				continue;
			}
			count += group.getBoxCount();
		}
		return count;
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

		for (RemainingBoxItemGroup loadableItemGroup : groupsMatrix) {
			if(loadableItemGroup == null) {
				continue;
			}

			List<RemainingBoxItem> items = loadableItemGroup.getItems();
			
			int count = loadableItemGroup.getBoxCount();
			
			if(count == 0) {
				continue;
			}
			
			int maxCount = 0;
			for (RemainingBoxItem value : items) {
				if(value != null) {
					if(maxCount < value.getCount()) {
						maxCount = value.getCount();
					}
				}
			}
	
			if(maxCount > 1) {
				int[] factors = new int[maxCount];
				for (RemainingBoxItem value : items) {
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
			RemainingBoxItemGroup boxItemGroup = groupsMatrix[i];
			if(boxItemGroup == null) {
				// excluded, i.e. does not fit the container
				continue;
			}
			for (RemainingBoxItem boxItem : boxItemGroup.getItems()) {
				count += boxItem.getCount();
				stackableItems[boxItem.getLocalIndex()] = null;
			}
			groupsMatrix[i] = null;
		}
		return count;
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		 for (Integer i : removed) {
			RemainingBoxItem boxItem = stackableItems[i];
			
			boxItem.decrement();
			
			if(boxItem.isEmpty()) {
				stackableItems[i] = null;
			}
		}
		
		for(int i = 0; i < groupsMatrix.length; i++) {
			RemainingBoxItemGroup group = groupsMatrix[i];
			if(group == null) {
				continue;
			}
			group.removeEmpty();
			if(group.isEmpty()) {
				groupsMatrix[i] = null;
			}
		}
		
	}
	
	public RemainingBoxItemGroup[] getBoxItemGroups() {
		return groupsMatrix;
	}
	
	public List<RemainingBoxItemGroup> getExcludedBoxItemGroups() {
		return excludedBoxItemGroups;
	}
	
}
