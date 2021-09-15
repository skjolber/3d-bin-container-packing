package com.github.skjolber.packing.impl.points3d;

/**
 * 
 * X is fixed within a Y-Z plane.
 * 
 */
public interface FixedPointX {

	int getFixedXMinY();
	int getFixedXMaxY();

	int getFixedXMinZ();
	int getFixedXMaxZ();

}
