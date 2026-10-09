package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box items which have been filtered.
 *
 * <p>All index arguments and the position returned by {@link #get(int)} are
 * local to this source. They can change when an item is removed. They are not
 * {@link BoxItem#getGlobalIndex() global box item indexes}.</p>
 *
 * <p>Implementations must keep {@link BoxItem#setLocalIndex(int)} in sync with the positions: after the source is built and
 * after every removal, each box item's {@link BoxItem#getLocalIndex() local index} is its position in the source. Packagers
 * pass {@code placement.getBoxItem().getLocalIndex()} back into {@link #decrement(int, int)}.</p>
 *
 * <p>Manifest controls filter the box items by removing them from the shared source.</p>
 * 
 */

public interface BoxItemSource extends Iterable<BoxItem> {
	
	int size();

	boolean isEmpty();

	/**
	 * Return the item at this source's current local index.
	 *
	 * @param localIndex current index in this source
	 */
	BoxItem get(int localIndex);

	/**
	 * Decrement the item at this source's current local index, i.e. reduce its count by the number of boxes which were placed.
	 * The item is removed from this source when none remain, so the local indexes of the following items change.
	 *
	 * @param localIndex current index in this source
	 * @param count the number of boxes to remove from the item
	 * @return true if this source still holds box items after the decrement, false if it is empty (the same as
	 *         {@code !isEmpty()}); packagers do not read the value
	 */
	boolean decrement(int localIndex, int count);
 
	/**
	 * Remove the item at this source's current local index. A source backed by box item groups also removes the item's group.
	 *
	 * @param localIndex current index in this source
	 * @return the removed item
	 */
	BoxItem remove(int localIndex);

	default long getMinVolume() {
		long minVolume = Long.MAX_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getVolume() < minVolume) {
				minVolume = box.getVolume();
			}
		}
		return minVolume;
	}
	
	default long getMinArea() {
		long minArea = Long.MAX_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getMinimumArea() < minArea) {
				minArea = box.getMinimumArea();
			}
		}
		return minArea;
	}
	
	default long getMaxVolume() {
		long maxVolume = Integer.MIN_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getVolume() > maxVolume) {
				maxVolume = box.getVolume();
			}
		}
		return maxVolume;
	}
	
	/**
	 * @return the largest minimum area (footprint) of the boxes
	 */
	default long getMaxArea() {
		long maxArea = Integer.MIN_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getMinimumArea() > maxArea) {
				maxArea = box.getMinimumArea();
			}
		}
		return maxArea;
	}
	
	/**
	 * @return the box item groups of the items in this source, or null if the source has no groups
	 */
	BoxItemGroupSource getGroups();
	
}
