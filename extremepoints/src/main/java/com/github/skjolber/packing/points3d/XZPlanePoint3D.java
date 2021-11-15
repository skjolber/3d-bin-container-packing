package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.api.Placement3D;

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
