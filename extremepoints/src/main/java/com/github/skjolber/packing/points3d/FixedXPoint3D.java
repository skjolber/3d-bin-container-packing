package com.github.skjolber.packing.points3d;

/**
 * 
 * X is fixed within a Y-Z plane.
 * 
 */
public interface FixedXPoint3D {

	int getFixedXMinY();
	int getFixedXMaxY();

	int getFixedXMinZ();
	int getFixedXMaxZ();

	boolean isFixedX(int y, int z);
}
