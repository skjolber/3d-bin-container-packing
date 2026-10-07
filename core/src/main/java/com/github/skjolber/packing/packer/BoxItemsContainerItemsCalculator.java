package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;

/**
 * Container inventory with an index of which container types can load each
 * box-item type. The remaining container count is capped at the number of
 * boxes, because using more containers cannot contribute to a packing.
 */
public final class BoxItemsContainerItemsCalculator extends ContainerItemsCalculator {

	private final boolean[][] fits;
	/**
	 * Box item global indexes in ascending order, and their fit record rows. Local indexes change when packaging
	 * filters box items, global indexes do not (but can be sparse).
	 */
	private final int[] sortedGlobalIndexes;
	private final int[] rowsBySortedGlobalIndex;
	private final int[] fittingContainerItemCounts;
	private final long resetRemainingVolume;
	private final long resetRemainingWeight;

	private long remainingVolume;
	private long remainingWeight;

	public BoxItemsContainerItemsCalculator(List<ContainerItem> containerItems, int containerCount, List<RemainingBoxItem> boxItems) {
		super(containerItems, Math.min(containerCount, countBoxes(boxItems)));
		this.remainingVolume = calculateVolume(boxItems);
		this.remainingWeight = calculateWeight(boxItems);
		this.resetRemainingVolume = remainingVolume;
		this.resetRemainingWeight = remainingWeight;
		this.fits = new boolean[boxItems.size()][containerItems.size()];
		this.sortedGlobalIndexes = new int[boxItems.size()];
		this.rowsBySortedGlobalIndex = new int[boxItems.size()];
		indexRows(boxItems, sortedGlobalIndexes, rowsBySortedGlobalIndex);
		this.fittingContainerItemCounts = new int[boxItems.size()];
		for(int boxItemIndex = 0; boxItemIndex < boxItems.size(); boxItemIndex++) {
			RemainingBoxItem boxItem = boxItems.get(boxItemIndex);
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
		this.sortedGlobalIndexes = source.sortedGlobalIndexes;
		this.rowsBySortedGlobalIndex = source.rowsBySortedGlobalIndex;
		this.fittingContainerItemCounts = source.fittingContainerItemCounts.clone();
	}

	private static void indexRows(List<RemainingBoxItem> boxItems, int[] sortedGlobalIndexes, int[] rows) {
		long[] keys = new long[boxItems.size()];
		for(int row = 0; row < boxItems.size(); row++) {
			// global index in the high bits, row in the low bits
			keys[row] = ((long)boxItems.get(row).getGlobalIndex() << 32) | row;
		}
		Arrays.sort(keys);
		for(int i = 0; i < keys.length; i++) {
			sortedGlobalIndexes[i] = (int)(keys[i] >> 32);
			rows[i] = (int)keys[i];
		}
	}

	/** @return the fit record row of the box item, or -1 if unknown */
	private int getRow(RemainingBoxItem boxItem) {
		int globalIndex = boxItem.getGlobalIndex();
		if(globalIndex < 0) {
			return -1;
		}
		int index = Arrays.binarySearch(sortedGlobalIndexes, globalIndex);
		return index >= 0 ? rowsBySortedGlobalIndex[index] : -1;
	}

	@Override
	public BoxItemsContainerItemsCalculator copy() {
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
	public boolean isFeasible(List<RemainingBoxItem> boxItems, int maxCount, boolean[] excluded) {
		long totalVolume = 0;
		long totalWeight = 0;
		int boxCount = 0;
		for(RemainingBoxItem boxItem : boxItems) {
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
		for(RemainingBoxItem boxItem : boxItems) {
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
	public boolean canLoad(RemainingBoxItem boxItem, int containerItemIndex) {
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

	private static int countBoxes(List<RemainingBoxItem> boxItems) {
		int count = 0;
		for(RemainingBoxItem boxItem : boxItems) {
			count += boxItem.getCount();
		}
		return count;
	}

	private static long calculateVolume(List<RemainingBoxItem> boxItems) {
		long volume = 0;
		for(RemainingBoxItem boxItem : boxItems) {
			volume += boxItem.getVolume();
		}
		return volume;
	}

	private static long calculateWeight(List<RemainingBoxItem> boxItems) {
		long weight = 0;
		for(RemainingBoxItem boxItem : boxItems) {
			weight += boxItem.getWeight();
		}
		return weight;
	}

}
