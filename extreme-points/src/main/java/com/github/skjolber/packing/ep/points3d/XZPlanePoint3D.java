package com.github.skjolber.packing.ep.points3d;

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
}
