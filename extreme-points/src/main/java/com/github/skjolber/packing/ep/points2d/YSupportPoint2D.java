package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.Placement2D;

/**
 * 
 * Point supported by a line in the y direction, effectively enforcing an x coordinate.
 *
 */

public interface YSupportPoint2D {

	Placement2D getYSupport();

}
