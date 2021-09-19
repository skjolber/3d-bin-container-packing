package com.github.skjolber.packing.points3d;

/**
 * 
 * Z is fixed within a X-Y plane.
 * 
 */

public interface FixedZPoint3D {

	int getFixedZMinX();
	int getFixedZMaxX();

	int getFixedZMinY();
	int getFixedZMaxY();
	
	boolean isFixedZ(int x, int y);

}
