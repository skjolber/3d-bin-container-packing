package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

public class PlainPlacementComparator implements Comparator<PlainPlacement> {

	@Override
	public int compare(PlainPlacement referenceResult, PlainPlacement potentiallyBetterResult) {
		int result = Long.compare(referenceResult.getBestPointSupportPercent(), potentiallyBetterResult.getBestPointSupportPercent());
		if(result != 0) {
			return result;
		}

		result = Integer.compare(referenceResult.getPoint().getMinZ(), potentiallyBetterResult.getPoint().getMinZ());
		if(result != 0) {
			return result;
		}

		return Long.compare(referenceResult.getStackValue().getArea(), potentiallyBetterResult.getStackValue().getArea());
	}
	
}