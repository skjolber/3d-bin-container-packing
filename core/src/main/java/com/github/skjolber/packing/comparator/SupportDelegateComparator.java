package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement;

public class SupportDelegateComparator implements Comparator<Placement> {
	
	private final Comparator<Placement> delegate; 

	public SupportDelegateComparator(Comparator<Placement> delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public int compare(Placement referenceResult, Placement potentiallyBetterResult) {
		
		// higher support is better
		long referenceValue = (referenceResult.getSupportedArea() * 1000) / referenceResult.getStackValue().getArea();
		long potentiallyBetterValue = (potentiallyBetterResult.getSupportedArea() * 1000) / potentiallyBetterResult.getStackValue().getArea();
		
		int result = Long.compare(referenceValue, potentiallyBetterValue);
		if(result != 0) {
			return result;
		}

		return delegate.compare(referenceResult, potentiallyBetterResult);
	}
	
}