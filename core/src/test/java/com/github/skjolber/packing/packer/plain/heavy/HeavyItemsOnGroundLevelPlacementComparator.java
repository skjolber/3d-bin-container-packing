package com.github.skjolber.packing.packer.plain.heavy;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.SupportDelegateComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;


/**
 * 
 * To make sure placement of a heavy box on the ground level trumps non-heavy boxes.
 * 
 */

public class HeavyItemsOnGroundLevelPlacementComparator implements PlacementComparator {
	
	protected final int maxWeight;

	public HeavyItemsOnGroundLevelPlacementComparator(int maxWeight) {
		this.maxWeight = maxWeight;
	}
	
	@Override
	public int compare(Placement referenceResult, Placement potentiallyBetterResult) {
		
		boolean heavyReference = referenceResult.getBox().getWeight() > maxWeight && referenceResult.getAbsoluteZ() == 0;
		boolean heavyPotentiallyBetter = potentiallyBetterResult.getBox().getWeight() > maxWeight && potentiallyBetterResult.getAbsoluteZ() == 0;
		
		if( (!heavyReference && !heavyPotentiallyBetter) || (heavyReference && heavyPotentiallyBetter)) {
			
			int result = Integer.compare(referenceResult.getAbsoluteZ(), potentiallyBetterResult.getAbsoluteZ());
			if(result != 0) {
				return result;
			}

			return Long.compare(referenceResult.getStackValue().getArea(), potentiallyBetterResult.getStackValue().getArea());
		}
		
		if(heavyReference && !heavyPotentiallyBetter) {
			return 1;
		} else {
			return -1;
		}
	}

}
