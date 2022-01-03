package com.github.skjolber.packing.api;

/**
 * 
 * X is fixed within a Y-Z plane.
 * 
 */
public interface YZPlanePoint3D {

	int getSupportedYZPlaneMinY();
	int getSupportedYZPlaneMaxY();

	int getSupportedYZPlaneMinZ();
	int getSupportedYZPlaneMaxZ();
	
	Placement3D getYZPlane();
}
