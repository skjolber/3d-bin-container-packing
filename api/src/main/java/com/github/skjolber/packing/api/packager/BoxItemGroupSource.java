package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

/**
 * 
 * List of box item groups which have been filtered.
 * 
 */

public interface BoxItemGroupSource extends Iterable<RemainingBoxItemGroup> {
	
	int size();
	
	boolean isEmpty();

	RemainingBoxItemGroup get(int index);

	RemainingBoxItemGroup remove(int index);
 
	default long getMinVolume() {
		long minVolume = Integer.MAX_VALUE;
		for(RemainingBoxItemGroup boxItemGroup: this) {
			for (RemainingBoxItem boxItem : boxItemGroup.getItems()) {
				if(boxItem.getBox().getVolume() < minVolume) {
					minVolume = boxItem.getBox().getVolume();
				}
			}
		}
		return minVolume;
	}

	default long getMinArea() {
		long minArea = Integer.MAX_VALUE;
		for(RemainingBoxItemGroup boxItemGroup: this) {
			for (RemainingBoxItem boxItem : boxItemGroup.getItems()) {
				if(boxItem.getBox().getMinimumArea() < minArea) {
					minArea = boxItem.getBox().getMinimumArea();
				}
			}
		}
		return minArea;
	}
}
