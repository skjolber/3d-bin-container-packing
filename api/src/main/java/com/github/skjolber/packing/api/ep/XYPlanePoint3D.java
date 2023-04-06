package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.Placement3D;

import java.math.BigDecimal;

/**
 * 
 * Z is fixed within a X-Y plane.
 * 
 */

public interface XYPlanePoint3D {

	BigDecimal getSupportedXYPlaneMinX();

	BigDecimal getSupportedXYPlaneMaxX();

	BigDecimal getSupportedXYPlaneMinY();

	BigDecimal getSupportedXYPlaneMaxY();

	Placement3D getXYPlane();
}
