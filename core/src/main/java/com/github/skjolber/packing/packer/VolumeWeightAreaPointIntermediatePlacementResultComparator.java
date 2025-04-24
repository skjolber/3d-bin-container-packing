package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public class VolumeWeightAreaPointIntermediatePlacementResultComparator implements Comparator<IntermediatePlacementResult> {

	@Override
	public int compare(IntermediatePlacementResult o1, IntermediatePlacementResult o2) {
		int result = Long.compare(o1.getStackValue().getVolume(), o2.getStackValue().getVolume());
		if(result != 0) {
			return result;
		}
		result = Long.compare(o1.getBoxItem().getBox().getWeight(), o2.getBoxItem().getBox().getWeight());
		if(result != 0) {
			return result;
		}
		// reversed: smaller area is better
		result = Long.compare(o2.getStackValue().getArea(), o1.getStackValue().getArea());
		if(result != 0) {
			return result;
		}
		// reversed: smaller point is better
		result = Long.compare(o2.getPoint().getVolume(), o1.getPoint().getVolume());
		if(result != 0) {
			return result;
		}
		return 0;
	}
	
}