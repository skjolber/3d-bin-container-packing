package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

/**
 * Container inventory with an index of which container types can load each
 * indivisible box-item group. The remaining container count is capped at the
 * number of groups, because using more containers cannot contribute to a
 * packing.
 */
public final class BoxItemGroupsContainerItemsCalculator extends ContainerItemsCalculator {

	private final boolean[][] fits;
	private final int[] fittingContainerItemCounts;
	private final long resetRemainingVolume;
	private final long resetRemainingWeight;

	private long remainingVolume;
	private long remainingWeight;

	public BoxItemGroupsContainerItemsCalculator(List<ControlledContainerItem> containerItems, int containerCount,
			List<BoxItemGroup> boxItemGroups) {
		super(containerItems, Math.min(containerCount, boxItemGroups.size()));
		this.remainingVolume = calculateVolume(boxItemGroups);
		this.remainingWeight = calculateWeight(boxItemGroups);
		this.resetRemainingVolume = remainingVolume;
		this.resetRemainingWeight = remainingWeight;
		this.fits = new boolean[boxItemGroups.size()][containerItems.size()];
		this.fittingContainerItemCounts = new int[boxItemGroups.size()];
		for(int groupIndex = 0; groupIndex < boxItemGroups.size(); groupIndex++) {
			BoxItemGroup group = boxItemGroups.get(groupIndex);
			for(int containerItemIndex = 0; containerItemIndex < containerItems.size(); containerItemIndex++) {
				ControlledContainerItem containerItem = containerItems.get(containerItemIndex);
				if(containerItem.getContainer().canLoad(group)) {
					fits[groupIndex][containerItemIndex] = true;
					if(containerItem.isAvailable()) {
						fittingContainerItemCounts[groupIndex]++;
					}
				}
			}
		}
	}

	private BoxItemGroupsContainerItemsCalculator(BoxItemGroupsContainerItemsCalculator source,
			List<ControlledContainerItem> containerItems) {
		super(containerItems, source.containerCount, source.resetContainerCount);
		this.cost = source.cost;
		this.remainingVolume = source.remainingVolume;
		this.remainingWeight = source.remainingWeight;
		this.resetRemainingVolume = source.resetRemainingVolume;
		this.resetRemainingWeight = source.resetRemainingWeight;
		// Immutable for the lifetime of the packaging operation; forks can share it.
		this.fits = source.fits;
		this.fittingContainerItemCounts = source.fittingContainerItemCounts.clone();
	}

	@Override
	public BoxItemGroupsContainerItemsCalculator clone() {
		return new BoxItemGroupsContainerItemsCalculator(this, copyContainerItems());
	}

	@Override
	public void reset() {
		super.reset();
		remainingVolume = resetRemainingVolume;
		remainingWeight = resetRemainingWeight;
		rebuildFittingContainerItemCounts();
	}

	@Override
	public Container toContainer(ContainerItem item, Stack stack) {
		int containerItemIndex = item.getIndex();
		boolean becomesUnavailable = item.getCount() == 1;
		Container result = super.toContainer(item, stack);
		remainingVolume -= stack.getVolume();
		remainingWeight -= stack.getWeight();
		if(becomesUnavailable) {
			for(int groupIndex = 0; groupIndex < fits.length; groupIndex++) {
				if(fits[groupIndex][containerItemIndex]) {
					fittingContainerItemCounts[groupIndex]--;
				}
			}
		}
		return result;
	}

	public int getBoxItemGroupCount() {
		return fits.length;
	}

	@Override
	public boolean isGroupFeasible(List<BoxItemGroup> groups, int maxCount, boolean[] excluded) {
		long totalVolume = 0;
		long totalWeight = 0;
		int groupCount = 0;
		for(BoxItemGroup group : groups) {
			if(group.isEmpty()) {
				continue;
			}
			totalVolume += group.getVolume();
			totalWeight += group.getWeight();
			groupCount++;
		}
		if(!hasCapacity(maxCount, groupCount, totalVolume, totalWeight, excluded)) {
			return false;
		}
		boolean excludedContainers = hasExclusions(excluded);
		for(BoxItemGroup group : groups) {
			if(group.isEmpty()) {
				continue;
			}
			int groupIndex = group.getIndex();
			if(groupIndex < 0 || groupIndex >= fits.length) {
				return super.isGroupFeasible(groups, maxCount, excluded);
			}
			if(!excludedContainers) {
				if(!hasContainer(groupIndex)) {
					return false;
				}
				continue;
			}
			boolean match = false;
			for(int containerItemIndex = 0; containerItemIndex < fits[groupIndex].length; containerItemIndex++) {
				if(!isExcluded(containerItemIndex, excluded) && fits[groupIndex][containerItemIndex]
						&& getContainerItem(containerItemIndex).isAvailable()) {
					match = true;
					break;
				}
			}
			if(!match) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canLoad(BoxItemGroup group, int containerItemIndex) {
		int groupIndex = group.getIndex();
		if(groupIndex >= 0 && groupIndex < fits.length) {
			return fits[groupIndex][containerItemIndex];
		}
		return super.canLoad(group, containerItemIndex);
	}

	public boolean canLoad(int groupIndex, int containerItemIndex) {
		return fits[groupIndex][containerItemIndex];
	}

	public boolean hasContainer(int groupIndex) {
		return containerCount > 0 && fittingContainerItemCounts[groupIndex] > 0;
	}

	public int getFittingContainerItemCount(int groupIndex) {
		return containerCount == 0 ? 0 : fittingContainerItemCounts[groupIndex];
	}

	public long getRemainingVolume() {
		return remainingVolume;
	}

	public long getRemainingWeight() {
		return remainingWeight;
	}

	private void rebuildFittingContainerItemCounts() {
		for(int groupIndex = 0; groupIndex < fits.length; groupIndex++) {
			int count = 0;
			for(int containerItemIndex = 0; containerItemIndex < fits[groupIndex].length; containerItemIndex++) {
				if(fits[groupIndex][containerItemIndex] && getContainerItem(containerItemIndex).isAvailable()) {
					count++;
				}
			}
			fittingContainerItemCounts[groupIndex] = count;
		}
	}

	private List<ControlledContainerItem> copyContainerItems() {
		List<ControlledContainerItem> copies = new ArrayList<>(containerItems.size());
		for(ControlledContainerItem item : containerItems) {
			copies.add(new ControlledContainerItem(item));
		}
		return copies;
	}

	private static long calculateVolume(List<BoxItemGroup> groups) {
		long volume = 0;
		for(BoxItemGroup group : groups) {
			volume += group.getVolume();
		}
		return volume;
	}

	private static long calculateWeight(List<BoxItemGroup> groups) {
		long weight = 0;
		for(BoxItemGroup group : groups) {
			weight += group.getWeight();
		}
		return weight;
	}

}
