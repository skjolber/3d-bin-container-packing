package com.github.skjolber.packing.comparator.placement;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;

public class VolumeWeightAreaMinZPlacementComparator implements PlacementComparator {

	@Override
	public boolean usesSupportedArea() {
		return false;
	}

	@Override
	public boolean prefersHigherSupportedArea() {
		return true;
	}

	@Override
	public int compare(Placement o1, Placement o2) {
		int result = Long.compare(o1.getStackValue().getVolume(), o2.getStackValue().getVolume());
		if(result != 0) {
			return result;
		}
		result = Long.compare(o1.getBox().getWeight(), o2.getBox().getWeight());
		if(result != 0) {
			return result;
		}
		// reversed: smaller area is better
		result = Long.compare(o2.getStackValue().getArea(), o1.getStackValue().getArea());
		if(result != 0) {
			return result;
		}

		// smaller z is better
		return Integer.compare(o2.getAbsoluteZ(), o1.getAbsoluteZ());
	}
	
}