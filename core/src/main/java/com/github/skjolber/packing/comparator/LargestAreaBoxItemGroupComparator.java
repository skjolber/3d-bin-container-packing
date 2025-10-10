package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class LargestAreaBoxItemGroupComparator implements Comparator<BoxItemGroup> {

	protected static final LargestAreaBoxItemGroupComparator INSTANCE = new LargestAreaBoxItemGroupComparator();
	
	public static LargestAreaBoxItemGroupComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(BoxItemGroup o1, BoxItemGroup o2) {
		// ****************************************
		// * Prefer the highest maximum area
		// ****************************************

		BoxItem o1Area = getMaximumArea(o1);
		BoxItem o2Area = getMaximumArea(o2);

		int compare = Long.compare(o1Area.getBox().getMaximumArea(), o2Area.getBox().getMaximumArea());
		if(compare != 0) {
			return compare;
		}

		compare = Long.compare(o1Area.getBox().getVolume(), o2Area.getBox().getVolume());
		if(compare != 0) {
			return compare;
		}

		compare = Long.compare(o1Area.getBox().getWeight(), o2Area.getBox().getWeight());
		if(compare != 0) {
			return compare;
		}

		return 0;
	}
	
	private BoxItem getMaximumArea(BoxItemGroup boxItemGroup) {
		BoxItem best = null;
		for(int i = 0; i < boxItemGroup.size(); i++) {
			BoxItem boxItem = boxItemGroup.get(i);
			
			long maximumArea = boxItem.getBox().getMaximumArea();
			if(best == null || best.getBox().getMaximumArea() < maximumArea) {
				best = boxItem;
			}
		}
		return best;
	}
	
}