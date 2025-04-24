package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItemGroup;

public class VolumeThenWeightBoxItemGroupComparator implements Comparator<BoxItemGroup> {

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