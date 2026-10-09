package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface BoxItemSource extends Iterable<BoxItem> {
	
	int size();

	boolean isEmpty();

	BoxItem get(int index);

	boolean decrement(int index, int count);
 
	BoxItem remove(int index);

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
	 * Get the largest minimum footprint, i.e. the area which the box with the largest footprint needs even in its most favourable orientation.
	 * 
	 * @return the largest of the boxes' minimum areas
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
	
	BoxItemGroupSource getGroups();
	
}
