package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.Placement3D;

import java.math.BigDecimal;

/**
 * 
 * Y is fixed within an x-z plane
 *
 */

public interface XZPlanePoint3D {

	BigDecimal getSupportedXZPlaneMinX();

	BigDecimal getSupportedXZPlaneMaxX();

	BigDecimal getSupportedXZPlaneMinZ();

	BigDecimal getSupportedXZPlaneMaxZ();

	Placement3D getXZPlane();
}
