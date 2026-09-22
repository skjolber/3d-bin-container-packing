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
	 * Decrement the item at this source's current local index.
	 *
	 * @param localIndex current index in this source
	 */
	boolean decrement(int localIndex, int count);
 
	/**
	 * Remove the item at this source's current local index.
	 *
	 * @param localIndex current index in this source
	 */
	BoxItem remove(int localIndex);

	default long getMinVolume() {
		long minVolume = Integer.MAX_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getVolume() < minVolume) {
				minVolume = box.getVolume();
			}
		}
		return minVolume;
	}
	
	default long getMinArea() {
		long minArea = Integer.MAX_VALUE;
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
	
	default long getMaxArea() {
		long maxArea = Integer.MIN_VALUE;
		for(BoxItem boxItem : this) {
			Box box = boxItem.getBox();
			if(box.getMinimumArea() > maxArea) {
				maxArea = box.getMaximumArea();
			}
		}
		return maxArea;
	}
	
	BoxItemGroupSource getGroups();
	
}
