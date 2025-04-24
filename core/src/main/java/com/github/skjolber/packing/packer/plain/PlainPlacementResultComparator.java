package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

public class PlainPlacementResultComparator implements Comparator<PlainIntermediatePlacementResult> {

	@Override
	public int compare(PlainIntermediatePlacementResult referenceResult, PlainIntermediatePlacementResult potentiallyBetterResult) {
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