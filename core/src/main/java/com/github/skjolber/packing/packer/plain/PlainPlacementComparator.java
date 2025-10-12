package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

public class PlainPlacementComparator implements Comparator<PlainPlacement> {

	@Override
	public int compare(PlainPlacement referenceResult, PlainPlacement potentiallyBetterResult) {
		
		long referenceValue = (referenceResult.getSupportArea() * 1000) / referenceResult.getStackValue().getArea();
		long potentiallyBetterValue = (potentiallyBetterResult.getSupportArea() * 1000) / potentiallyBetterResult.getStackValue().getArea();
		
		int result = Long.compare(referenceValue, potentiallyBetterValue);
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