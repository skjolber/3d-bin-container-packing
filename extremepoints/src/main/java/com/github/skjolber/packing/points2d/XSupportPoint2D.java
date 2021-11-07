package com.github.skjolber.packing.points2d;

import com.github.skjolber.packing.api.Placement2D;

/**
 * 
 * Point supported by a line in the x direction, effectively enforcing a y coordinate.
 *
 */

public interface XSupportPoint2D {

	Placement2D getXSupport();

}
