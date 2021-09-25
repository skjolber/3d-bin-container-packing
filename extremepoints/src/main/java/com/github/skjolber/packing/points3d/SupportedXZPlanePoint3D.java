package com.github.skjolber.packing.points3d;

/**
 * 
 * Y is fixed within an x-z plane
 *
 */

public interface SupportedXZPlanePoint3D {

	int getSupportedXZPlaneMinX();
	int getSupportedXZPlaneMaxX();

	int getSupportedXZPlaneMinZ();
	int getSupportedXZPlaneMaxZ();

}
