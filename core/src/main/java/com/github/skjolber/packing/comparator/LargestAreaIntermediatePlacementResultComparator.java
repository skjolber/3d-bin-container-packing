package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public class LargestAreaIntermediatePlacementResultComparator implements Comparator<IntermediatePlacementResult> {

	@Override
	public int compare(IntermediatePlacementResult o1, IntermediatePlacementResult o2) {
		
		// ****************************************
		// * Prefer the highest area
		// ****************************************

		BoxStackValue o1StackValue = o1.getStackValue();
		BoxStackValue o2StackValue = o2.getStackValue();
		
		int compare = Long.compare(o1StackValue.getArea(), o2StackValue.getArea());
		if(compare != 0) {
			return compare;
		}

		compare = Long.compare(o1StackValue.getVolume(), o2StackValue.getVolume());
		if(compare != 0) {
			return compare;
		}
		
		compare = Long.compare(o1.getBoxItem().getBox().getWeight(), o2.getBoxItem().getBox().getWeight());
		if(compare != 0) {
			return compare;
		}

		return 0;
	}
	
}