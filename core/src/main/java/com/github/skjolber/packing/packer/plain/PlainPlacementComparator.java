package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement;

public class PlainPlacementComparator implements Comparator<Placement> {

	@Override
	public int compare(Placement referenceResult, Placement potentiallyBetterResult) {
		
		if(referenceResult instanceof PlainPlacement && potentiallyBetterResult instanceof PlainPlacement) {
			PlainPlacement referenceResultPlainPlacement = (PlainPlacement)referenceResult;
			PlainPlacement potentiallyBetterResultPlainPlacement = (PlainPlacement)potentiallyBetterResult;
			
			long referenceValue = (referenceResultPlainPlacement.getSupportArea() * 1000) / referenceResult.getStackValue().getArea();
			long potentiallyBetterValue = (potentiallyBetterResultPlainPlacement.getSupportArea() * 1000) / potentiallyBetterResult.getStackValue().getArea();
			
			int result = Long.compare(referenceValue, potentiallyBetterValue);
			if(result != 0) {
				return result;
			}
		}

		int result = Integer.compare(referenceResult.getPoint().getMinZ(), potentiallyBetterResult.getPoint().getMinZ());
		if(result != 0) {
			return result;
		}

		return Long.compare(referenceResult.getStackValue().getArea(), potentiallyBetterResult.getStackValue().getArea());
	}
	
}