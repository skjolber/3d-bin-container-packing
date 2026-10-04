package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

/**
 * Container inventory with an index of which container types can load each
 * box-item type. The remaining container count is capped at the number of
 * boxes, because using more containers cannot contribute to a packing.
 */
public final class BoxItemsContainerItemsCalculator extends ContainerItemsCalculator {

	private final boolean[][] fits;
	/** Fit record row by box item global index (local indexes change when packaging filters box items), or -1 */
	private final int[] rowsByGlobalIndex;
	private final int[] fittingContainerItemCounts;
	private final long resetRemainingVolume;
	private final long resetRemainingWeight;

	private long remainingVolume;
	private long remainingWeight;

	public BoxItemsContainerItemsCalculator(List<ContainerItem> containerItems, int containerCount, List<BoxItem> boxItems) {
		super(containerItems, Math.min(containerCount, countBoxes(boxItems)));
		this.remainingVolume = calculateVolume(boxItems);
		this.remainingWeight = calculateWeight(boxItems);
		this.resetRemainingVolume = remainingVolume;
		this.resetRemainingWeight = remainingWeight;
		this.fits = new boolean[boxItems.size()][containerItems.size()];
		this.rowsByGlobalIndex = getRowsByGlobalIndex(boxItems);
		this.fittingContainerItemCounts = new int[boxItems.size()];
		for(int boxItemIndex = 0; boxItemIndex < boxItems.size(); boxItemIndex++) {
			BoxItem boxItem = boxItems.get(boxItemIndex);
			for(int containerItemIndex = 0; containerItemIndex < containerItems.size(); containerItemIndex++) {
				ContainerItem containerItem = containerItems.get(containerItemIndex);
				if(containerItem.getContainer().canLoad(boxItem.getBox())) {
					fits[boxItemIndex][containerItemIndex] = true;
					if(containerItem.isAvailable()) {
						fittingContainerItemCounts[boxItemIndex]++;
					}
				}
			}
		}
	}

	private BoxItemsContainerItemsCalculator(BoxItemsContainerItemsCalculator source, List<ContainerItem> containerItems) {
		super(containerItems, source.containerCount, source.resetContainerCount);
		this.cost = source.cost;
		this.remainingVolume = source.remainingVolume;
		this.remainingWeight = source.remainingWeight;
		this.resetRemainingVolume = source.resetRemainingVolume;
		this.resetRemainingWeight = source.resetRemainingWeight;
		// Immutable for the lifetime of the packaging operation; forks can share it.
		this.fits = source.fits;
		this.rowsByGlobalIndex = source.rowsByGlobalIndex;
		this.fittingContainerItemCounts = source.fittingContainerItemCounts.clone();
	}

	private static int[] getRowsByGlobalIndex(List<BoxItem> boxItems) {
		int maxGlobalIndex = -1;
		for(BoxItem boxItem : boxItems) {
			maxGlobalIndex = Math.max(maxGlobalIndex, boxItem.getGlobalIndex());
		}
		int[] rows = new int[maxGlobalIndex + 1];
		Arrays.fill(rows, -1);
		for(int row = 0; row < boxItems.size(); row++) {
			int globalIndex = boxItems.get(row).getGlobalIndex();
			if(globalIndex >= 0) {
				rows[globalIndex] = row;
			}
		}
		return rows;
	}

	/** @return the fit record row of the box item, or -1 if unknown */
	private int getRow(BoxItem boxItem) {
		int globalIndex = boxItem.getGlobalIndex();
		if(globalIndex >= 0 && globalIndex < rowsByGlobalIndex.length) {
			return rowsByGlobalIndex[globalIndex];
		}
		return -1;
	}

	@Override
	public BoxItemsContainerItemsCalculator clone() {
		return new BoxItemsContainerItemsCalculator(this, copyContainerItems());
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
			for(int boxItemIndex = 0; boxItemIndex < fits.length; boxItemIndex++) {
				if(fits[boxItemIndex][containerItemIndex]) {
					fittingContainerItemCounts[boxItemIndex]--;
				}
			}
		}
		return result;
	}

	public int getBoxItemCount() {
		return fits.length;
	}

	@Override
	public boolean isFeasible(List<BoxItem> boxItems, int maxCount, boolean[] excluded) {
		long totalVolume = 0;
		long totalWeight = 0;
		int boxCount = 0;
		for(BoxItem boxItem : boxItems) {
			if(boxItem.isEmpty()) {
				continue;
			}
			totalVolume += boxItem.getVolume();
			totalWeight += boxItem.getWeight();
			boxCount += boxItem.getCount();
		}
		if(!hasCapacity(maxCount, boxCount, totalVolume, totalWeight, excluded)) {
			return false;
		}
		boolean excludedContainers = hasExclusions(excluded);
		for(BoxItem boxItem : boxItems) {
			if(boxItem.isEmpty()) {
				continue;
			}
			int boxItemIndex = getRow(boxItem);
			if(boxItemIndex < 0) {
				return super.isFeasible(boxItems, maxCount, excluded);
			}
			if(!excludedContainers) {
				if(!hasContainer(boxItemIndex)) {
					return false;
				}
				continue;
			}
			boolean match = false;
			for(int containerItemIndex = 0; containerItemIndex < fits[boxItemIndex].length; containerItemIndex++) {
				if(!isExcluded(containerItemIndex, excluded) && fits[boxItemIndex][containerItemIndex]
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
	public boolean canLoad(BoxItem boxItem, int containerItemIndex) {
		int boxItemIndex = getRow(boxItem);
		if(boxItemIndex >= 0) {
			return fits[boxItemIndex][containerItemIndex];
		}
		return super.canLoad(boxItem, containerItemIndex);
	}

	public boolean canLoad(int boxItemIndex, int containerItemIndex) {
		return fits[boxItemIndex][containerItemIndex];
	}

	public boolean hasContainer(int boxItemIndex) {
		return containerCount > 0 && fittingContainerItemCounts[boxItemIndex] > 0;
	}

	public int getFittingContainerItemCount(int boxItemIndex) {
		return containerCount == 0 ? 0 : fittingContainerItemCounts[boxItemIndex];
	}

	public long getRemainingVolume() {
		return remainingVolume;
	}

	public long getRemainingWeight() {
		return remainingWeight;
	}

	private void rebuildFittingContainerItemCounts() {
		for(int boxItemIndex = 0; boxItemIndex < fits.length; boxItemIndex++) {
			int count = 0;
			for(int containerItemIndex = 0; containerItemIndex < fits[boxItemIndex].length; containerItemIndex++) {
				if(fits[boxItemIndex][containerItemIndex] && getContainerItem(containerItemIndex).isAvailable()) {
					count++;
				}
			}
			fittingContainerItemCounts[boxItemIndex] = count;
		}
	}

	private List<ContainerItem> copyContainerItems() {
		List<ContainerItem> copies = new ArrayList<>(containerItems.size());
		for(ContainerItem item : containerItems) {
			copies.add(new ContainerItem(item));
		}
		return copies;
	}

	private static int countBoxes(List<BoxItem> boxItems) {
		int count = 0;
		for(BoxItem boxItem : boxItems) {
			count += boxItem.getCount();
		}
		return count;
	}

	private static long calculateVolume(List<BoxItem> boxItems) {
		long volume = 0;
		for(BoxItem boxItem : boxItems) {
			volume += boxItem.getVolume();
		}
		return volume;
	}

	private static long calculateWeight(List<BoxItem> boxItems) {
		long weight = 0;
		for(BoxItem boxItem : boxItems) {
			weight += boxItem.getWeight();
		}
		return weight;
	}

}
