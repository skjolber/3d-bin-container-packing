package com.github.skjolber.packing.points3d;

/**
 * 
 * Z is fixed within a X-Y plane.
 * 
 */

public interface SupportedXYPlanePoint3D {

	int getSupportedXYPlaneMinX();
	int getSupportedXYPlaneMaxX();

	int getSupportedXYPlaneMinY();
	int getSupportedXYPlaneMaxY();
	
}
