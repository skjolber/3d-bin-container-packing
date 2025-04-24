package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItems extends Iterable<BoxItem> {
	
	int size();

	boolean isEmpty();

	BoxItem get(int index);

	BoxItem decrement(int index, int count);
 
	BoxItem remove(int index);

	void removeEmpty();

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
	
}
