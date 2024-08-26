package com.github.skjolber.packing.api.ep;
import java.util.List;

import com.github.skjolber.packing.api.ep.Point3D;

/**
 * Constraint describing where a box can be placed within a stack. 
 * For example, a flammable item must be stacked by the door.
 */

public interface Point3DFilter {

	List<Point3D> filter(List<Point3D> points);

}