package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItemGroup;

public class VolumeThenWeightBoxItemGroupComparator implements Comparator<BoxItemGroup> {

	protected static final VolumeThenWeightBoxItemGroupComparator INSTANCE = new VolumeThenWeightBoxItemGroupComparator();
	
	public static VolumeThenWeightBoxItemGroupComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(BoxItemGroup referenceBoxItemGroup, BoxItemGroup potentiallyBetterBoxItemGroup) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************

		if(referenceBoxItemGroup.getVolume() == potentiallyBetterBoxItemGroup.getVolume()) {
			return Long.compare(referenceBoxItemGroup.getWeight(), potentiallyBetterBoxItemGroup.getWeight());
		}
		return Long.compare(referenceBoxItemGroup.getVolume(), potentiallyBetterBoxItemGroup.getVolume());
	}
}