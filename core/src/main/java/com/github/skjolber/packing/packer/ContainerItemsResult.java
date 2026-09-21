package com.github.skjolber.packing.packer;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * Eligible container-item indexes and the item-to-container fit records produced
 * while calculating them.
 * <p>
 * Item indexes refer to the order of the box items or box-item groups supplied
 * to {@link ContainerItemsCalculator}. Container-item indexes refer to the
 * calculator's container inventory.
 */
public final class ContainerItemsResult extends AbstractList<Integer> implements RandomAccess {

	private final List<Integer> containerIndexes;
	private final boolean[][] fits;
	private final int containerItemCount;

	ContainerItemsResult(List<Integer> containerIndexes, boolean[][] fits, int containerItemCount) {
		this.containerIndexes = containerIndexes;
		this.fits = fits;
		this.containerItemCount = containerItemCount;
	}

	@Override
	public Integer get(int index) {
		return containerIndexes.get(index);
	}

	@Override
	public int size() {
		return containerIndexes.size();
	}

	@Override
	public Integer set(int index, Integer element) {
		return containerIndexes.set(index, element);
	}

	@Override
	public void add(int index, Integer element) {
		containerIndexes.add(index, element);
		modCount++;
	}

	@Override
	public Integer remove(int index) {
		Integer removed = containerIndexes.remove(index);
		modCount++;
		return removed;
	}

	/** Return the mutable eligible container-index list. */
	public List<Integer> getContainerIndexes() {
		return this;
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
		for(int containerItemIndex : containerIndexes) {
			if(fits[itemIndex][containerItemIndex]) {
				return true;
			}
		}
		return false;
	}

	/** Return the number of currently eligible container types which can load the item. */
	public int getFittingContainerItemCount(int itemIndex) {
		int count = 0;
		for(int containerItemIndex : containerIndexes) {
			if(fits[itemIndex][containerItemIndex]) {
				count++;
			}
		}
		return count;
	}
}
