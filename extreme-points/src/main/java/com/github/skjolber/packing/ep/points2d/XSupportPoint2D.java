package com.github.skjolber.packing.ep.points2d;

/**
 * 
 * Point supported by a line in the x direction, effectively enforcing a y coordinate.
 *
 */

public interface XSupportPoint2D {

	int getSupportedMinX();

	int getSupportedMaxX();
	
}
