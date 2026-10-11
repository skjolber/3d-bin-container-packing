package com.github.skjolber.packing.comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemComparator;

public class LargestAreaBoxItemComparator implements BoxItemComparator {

	protected static final LargestAreaBoxItemComparator INSTANCE = new LargestAreaBoxItemComparator();
	
	public static LargestAreaBoxItemComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(BoxItem o1, BoxItem o2) {
		// ****************************************
		// * Prefer the highest maximum area
		// ****************************************

		int compare = Long.compare(o1.getBox().getMaximumArea(), o2.getBox().getMaximumArea());
		if(compare != 0) {
			return compare;
		}

		compare = Long.compare(o1.getBox().getVolume(), o2.getBox().getVolume());
		if(compare != 0) {
			return compare;
		}

		compare = Long.compare(o1.getBox().getWeight(), o2.getBox().getWeight());
		if(compare != 0) {
			return compare;
		}

		return 0;
	}
	
}
