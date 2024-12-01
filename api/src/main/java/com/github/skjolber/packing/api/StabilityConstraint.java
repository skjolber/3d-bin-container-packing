package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.ep.Point3D;

/**
 * 
 * Interface for determining stacking stability (both below and sideways). 
 *  
 */

@SuppressWarnings("unchecked")
public interface StabilityConstraint {
	
	boolean isXyPlane();

	boolean isXzPlane();

	boolean isYzPlane();
	
	boolean accepts(Point3D point, List<StackPlacement> xyPlane, List<StackPlacement> xzPlane, List<StackPlacement> yzPlane);

}
