package com.github.skjolber.packing.comparator;

import com.github.skjolber.packing.api.Placement;

public class LowerZDelegatePlacementComparator implements PlacementComparator {

	private PlacementComparator delegate;

	public LowerZDelegatePlacementComparator(PlacementComparator delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public int compare(Placement o1, Placement o2) {

		// ****************************************
		// * Prefer lower z
		// ****************************************

		int compare = Long.compare(o1.getAbsoluteZ(), o2.getAbsoluteZ());
		if(compare != 0) {
			return compare;
		}

		return delegate.compare(o1, o2);
	}

}