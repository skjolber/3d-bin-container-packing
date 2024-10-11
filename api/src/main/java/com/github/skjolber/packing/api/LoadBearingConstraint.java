package com.github.skjolber.packing.api;

import com.github.skjolber.packing.api.ep.Point3D;

public interface LoadBearingConstraint {

	List<LoadBearingStackableConstraintType> getTypes();
	
	void loaded(StackPlacement stackPlacement);
	
	boolean canLoad(StackValue stackValue, Point3D point);
}
