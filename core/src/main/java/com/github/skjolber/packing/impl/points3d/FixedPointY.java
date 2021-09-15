package com.github.skjolber.packing.impl.points3d;

/**
 * 
 * Y is fixed within an x-z plane
 *
 */

public interface FixedPointY {

	int getFixedYMinX();
	int getFixedYMaxX();

	int getFixedYMinZ();
	int getFixedYMaxZ();
}
