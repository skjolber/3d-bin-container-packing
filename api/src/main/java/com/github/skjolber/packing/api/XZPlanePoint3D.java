package com.github.skjolber.packing.api;

/**
 * 
 * Y is fixed within an x-z plane
 *
 */

public interface XZPlanePoint3D {

	int getSupportedXZPlaneMinX();
	int getSupportedXZPlaneMaxX();

	int getSupportedXZPlaneMinZ();
	int getSupportedXZPlaneMaxZ();

	Placement3D getXZPlane();
}
