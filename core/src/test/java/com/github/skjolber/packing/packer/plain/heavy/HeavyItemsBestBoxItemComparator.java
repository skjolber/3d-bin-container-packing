package com.github.skjolber.packing.packer.plain.heavy;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;

/**
 * 
 * To make sure a heavy box can be better than a non-heavy box even with lower volume.
 * 
 */
public class HeavyItemsBestBoxItemComparator extends VolumeThenWeightBoxItemComparator {
	
	protected final int maxWeight;

	public HeavyItemsBestBoxItemComparator(int maxWeight) {
		super();
		this.maxWeight = maxWeight;
	}
	
	@Override
	public int compare(BoxItem referenceBoxItem, BoxItem potentiallyBetterBoxItem) {
		
		boolean heavyReference = referenceBoxItem.getBox().getWeight() > maxWeight;
		boolean heavyPotentiallyBetter = potentiallyBetterBoxItem.getBox().getWeight() > maxWeight;
		
		if( (!heavyReference && !heavyPotentiallyBetter) || (heavyReference && heavyPotentiallyBetter)) {
			return super.compare(referenceBoxItem, potentiallyBetterBoxItem);
		}
		
		if(heavyReference && !heavyPotentiallyBetter) {
			return 1;
		} else {
			return -1;
		}
	}

}
