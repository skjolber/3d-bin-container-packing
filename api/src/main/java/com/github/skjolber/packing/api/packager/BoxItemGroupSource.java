package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * List of box item groups which have been filtered.
 * 
 */

public interface BoxItemGroupSource extends Iterable<BoxItemGroup> {
	
	int size();
	
	BoxItemGroup get(int index);

	BoxItemGroup remove(int index);
 
	boolean isEmpty();

	default long getMinVolume() {
		long minVolume = Integer.MAX_VALUE;
		for(BoxItemGroup boxItemGroup: this) {
			for (BoxItem boxItem : boxItemGroup.getItems()) {
				if(boxItem.getBox().getVolume() < minVolume) {
					minVolume = boxItem.getBox().getVolume();
				}
			}
		}
		return minVolume;
	}

	default long getMinArea() {
		long minArea = Integer.MAX_VALUE;
		for(BoxItemGroup boxItemGroup: this) {
			for (BoxItem boxItem : boxItemGroup.getItems()) {
				if(boxItem.getBox().getMinimumArea() < minArea) {
					minArea = boxItem.getBox().getMinimumArea();
				}
			}
		}
		return minArea;
	}
}
