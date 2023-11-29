package com.github.skjolber.packing.ep.points2d;

/**
 * 
 * Point supported by a line in the y direction, effectively enforcing an x coordinate.
 *
 */

public interface YSupportPoint2D {

	int getSupportedMinY();

	int getSupportedMaxY();


}
