package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

public class VolumeThenWeightBoxItemComparator implements Comparator<BoxItem> {

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