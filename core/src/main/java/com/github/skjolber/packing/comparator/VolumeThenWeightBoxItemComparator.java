package com.github.skjolber.packing.comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemComparator;

public class VolumeThenWeightBoxItemComparator implements BoxItemComparator {

	protected static final VolumeThenWeightBoxItemComparator INSTANCE = new VolumeThenWeightBoxItemComparator();
	
	public static VolumeThenWeightBoxItemComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(BoxItem referenceBoxItem, BoxItem potentiallyBetterBoxItem) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************

		Box referenceBox = referenceBoxItem.getBox();
		Box potentiallyBetterBox = potentiallyBetterBoxItem.getBox();
		
		if(referenceBox.getVolume() == potentiallyBetterBox.getVolume()) {
			return Long.compare(referenceBox.getWeight(), potentiallyBetterBox.getWeight());
		}
		return Long.compare(referenceBox.getVolume(), potentiallyBetterBox.getVolume());
	}
}