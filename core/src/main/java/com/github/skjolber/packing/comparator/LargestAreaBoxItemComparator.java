package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class LargestAreaBoxItemComparator implements Comparator<BoxItem> {

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