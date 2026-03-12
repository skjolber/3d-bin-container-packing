package com.github.skjolber.packing.packer.plain.heavy;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.plain.PlainPlacementComparator;


/**
 * 
 * To make sure placement of a heavy box on the ground level trumps non-heavy boxes.
 * 
 */

public class HeavyItemsOnGroundLevelPlacementComparator extends PlainPlacementComparator {
	
	protected final int maxWeight;

	public HeavyItemsOnGroundLevelPlacementComparator(int maxWeight) {
		super();
		this.maxWeight = maxWeight;
	}
	
	@Override
	public int compare(Placement referenceResult, Placement potentiallyBetterResult) {
		
		boolean heavyReference = referenceResult.getBox().getWeight() > maxWeight && referenceResult.getPoint().getMinZ() == 0;
		boolean heavyPotentiallyBetter = potentiallyBetterResult.getBox().getWeight() > maxWeight && potentiallyBetterResult.getPoint().getMinZ() == 0;
		
		if( (!heavyReference && !heavyPotentiallyBetter) || (heavyReference && heavyPotentiallyBetter)) {
			return super.compare(referenceResult, potentiallyBetterResult);
		}
		
		if(heavyReference && !heavyPotentiallyBetter) {
			return 1;
		} else {
			return -1;
		}
	}

}
