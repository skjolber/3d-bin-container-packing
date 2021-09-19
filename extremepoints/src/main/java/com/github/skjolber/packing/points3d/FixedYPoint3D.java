package com.github.skjolber.packing.points3d;

/**
 * 
 * Y is fixed within an x-z plane
 *
 */

public interface FixedYPoint3D {

	int getFixedYMinX();
	int getFixedYMaxX();

	int getFixedYMinZ();
	int getFixedYMaxZ();
	
	boolean isFixedY(int x, int z);

}
