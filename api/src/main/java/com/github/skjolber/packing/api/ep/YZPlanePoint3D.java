package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.Placement3D;

import java.math.BigDecimal;

/**
 * 
 * X is fixed within a Y-Z plane.
 * 
 */
public interface YZPlanePoint3D {

	BigDecimal getSupportedYZPlaneMinY();

	BigDecimal getSupportedYZPlaneMaxY();

	BigDecimal getSupportedYZPlaneMinZ();

	BigDecimal getSupportedYZPlaneMaxZ();

	Placement3D getYZPlane();
}
