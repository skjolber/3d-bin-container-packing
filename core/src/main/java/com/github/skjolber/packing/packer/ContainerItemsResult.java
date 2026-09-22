package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

import com.github.skjolber.packing.api.ContainerItem;
/**
 * Eligible controlled container items and the item-to-container fit records produced
 * while calculating them.
 * <p>
 * Item indexes refer to the order of the box items or box-item groups supplied
 * to {@link ContainerItemsCalculator}. Container-item indexes refer to the
 * calculator's container inventory.
 */
public final class ContainerItemsResult extends AbstractList<ContainerItem> implements RandomAccess {

	private final List<ContainerItem> containerItems;
	private final boolean[][] fits;
	private final int containerItemCount;

	ContainerItemsResult(List<ContainerItem> containerItems, boolean[][] fits, int containerItemCount) {
		this.containerItems = containerItems;
		this.fits = fits;
		this.containerItemCount = containerItemCount;
	}

	@Override
	public ContainerItem get(int index) {
		return containerItems.get(index);
	}

	@Override
	public int size() {
		return containerItems.size();
	}

	@Override
	public ContainerItem set(int index, ContainerItem element) {
		return containerItems.set(index, element);
	}

	@Override
	public void add(int index, ContainerItem element) {
		containerItems.add(index, element);
		modCount++;
	}

	@Override
	public ContainerItem remove(int index) {
		ContainerItem removed = containerItems.remove(index);
		modCount++;
		return removed;
	}

	/** Return the indexes of the eligible container items for legacy index-based callers. */
	public List<Integer> getContainerIndexes() {
		List<Integer> indexes = new ArrayList<>(containerItems.size());
		for(ContainerItem containerItem : containerItems) {
			indexes.add(containerItem.getIndex());
		}
		return indexes;
	}

	/** Return the number of box items or box-item groups represented by this result. */
	public int getItemCount() {
		return fits.length;
	}

	/** Return the number of container types represented by the fit records. */
	public int getContainerItemCount() {
		return containerItemCount;
	}

	/**
	 * Return whether the item at {@code itemIndex} fits the container type at
	 * {@code containerItemIndex}. Unavailable container types are recorded as not
	 * fitting.
	 */
	public boolean canLoad(int itemIndex, int containerItemIndex) {
		return fits[itemIndex][containerItemIndex];
	}

	/** Return whether at least one currently eligible container can load the item. */
	public boolean hasContainer(int itemIndex) {
		for(ContainerItem containerItem : containerItems) {
			if(fits[itemIndex][containerItem.getIndex()]) {
				return true;
			}
		}
		return false;
	}

	/** Return the number of currently eligible container types which can load the item. */
	public int getFittingContainerItemCount(int itemIndex) {
		int count = 0;
		for(ContainerItem containerItem : containerItems) {
			if(fits[itemIndex][containerItem.getIndex()]) {
				count++;
			}
		}
		return count;
	}
}
