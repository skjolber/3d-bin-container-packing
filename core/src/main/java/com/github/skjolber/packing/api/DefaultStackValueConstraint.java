package com.github.skjolber.packing.api;

public class DefaultStackValueConstraint implements StackConstraint {

	// structural integrity for this orientation
	protected final int maxSupportedWeight;
	
	public DefaultStackValueConstraint(int maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}

	@Override
	public boolean supports(int weight, long area) {
		return weight <= maxSupportedWeight;
	}
	
}
