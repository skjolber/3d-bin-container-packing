package com.github.skjolber.packing.api;

public interface StackValueConstraint {

	LoadBearingConstraintBuilder<?> newLoadBearingConstraintBuilder();
	
	StabilityConstraintBuilder<?> newStabilityConstraint();
	
}
