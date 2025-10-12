package com.github.skjolber.packing.ep.points3d;

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

}
