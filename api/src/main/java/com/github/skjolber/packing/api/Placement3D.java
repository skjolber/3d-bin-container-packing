package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public interface Placement3D extends Placement2D {

	BigDecimal getAbsoluteZ();

	BigDecimal getAbsoluteEndZ();

	boolean intersects3D(Placement3D point);

	// List<? extends Placement3D> getSupports3D();

}
