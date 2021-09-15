package com.github.skjolber.packing.impl.points3d;

/**
 * 
 * Z is fixed within a X-Y plane.
 * 
 */

public interface FixedPointZ {

	int getFixedZMinX();
	int getFixedZMaxX();

	int getFixedZMinY();
	int getFixedZMaxY();
}
