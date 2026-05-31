package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement;

public class VolumeWeightAreaMinZPlacementComparator implements Comparator<Placement> {

	@Override
	public int compare(Placement o1, Placement o2) {
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

		// smaller z is better
		return Integer.compare(o1.getAbsoluteZ(), o2.getAbsoluteZ());
	}
	
}