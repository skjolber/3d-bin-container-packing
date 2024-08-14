package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.ep.Point3D;

/**
 * Constraint describing where a box can be placed within a stack. 
 * For example, a flammable item must be stacked by the door.
 */

public interface StackablePointFilter {

	List<Point3D> filterPoints(List<Point3D> points);
	
}
