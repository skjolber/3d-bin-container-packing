package com.github.skjolber.packing.comparator;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

public class SupportDelegateComparator implements PlacementComparator {

	private final PlacementComparator delegate;

	public SupportDelegateComparator(PlacementComparator delegate) {
		this.delegate = delegate;
	}

	@Override
	public int compare(Placement referenceResult, Placement potentiallyBetterResult) {

		// higher support is better
		long referenceValue = (referenceResult.getSupportedArea() * 1000)
				/ referenceResult.getStackValue().getArea();
		long potentiallyBetterValue = (potentiallyBetterResult.getSupportedArea() * 1000)
				/ potentiallyBetterResult.getStackValue().getArea();

		int result = Long.compare(referenceValue, potentiallyBetterValue);
		if(result != 0) {
			return result;
		}

		return delegate.compare(referenceResult, potentiallyBetterResult);
	}

}