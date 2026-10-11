package com.github.skjolber.packing.test.example;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;

/**
 * Example placement comparator, which depends on the api module only: load a container from the back
 * wall (x = 0) towards the doors, bottom up, i.e. prefer lower x, then lower z, then lower y.
 */
public class BackToFrontPlacementComparator implements PlacementComparator {

	@Override
	public int compare(Placement a, Placement b) {
		// a positive value means a is better
		int result = Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());
		if(result != 0) {
			return result;
		}
		result = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
		if(result != 0) {
			return result;
		}
		return Integer.compare(b.getAbsoluteY(), a.getAbsoluteY());
	}

	@Override
	public boolean usesSupportedArea() {
		// only positions are compared, so support and load need only be calculated for the best candidate
		return false;
	}
}
