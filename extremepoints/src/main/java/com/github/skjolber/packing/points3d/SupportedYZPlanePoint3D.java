package com.github.skjolber.packing.points3d;

/**
 * 
 * X is fixed within a Y-Z plane.
 * 
 */
public interface SupportedYZPlanePoint3D {

	int getSupportedYZPlaneMinY();
	int getSupportedYZPlaneMaxY();

	int getSupportedYZPlaneMinZ();
	int getSupportedYZPlaneMaxZ();
}
