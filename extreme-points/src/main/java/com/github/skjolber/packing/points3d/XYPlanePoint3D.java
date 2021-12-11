package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.api.Placement3D;

/**
 * 
 * Z is fixed within a X-Y plane.
 * 
 */

public interface XYPlanePoint3D {

	int getSupportedXYPlaneMinX();
	int getSupportedXYPlaneMaxX();

	int getSupportedXYPlaneMinY();
	int getSupportedXYPlaneMaxY();
	
	Placement3D getXYPlane();
}
