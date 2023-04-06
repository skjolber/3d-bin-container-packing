package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public interface Placement2D {

	BigDecimal getAbsoluteX();

	BigDecimal getAbsoluteY();

	BigDecimal getAbsoluteEndX();

	BigDecimal getAbsoluteEndY();

	boolean intersects2D(Placement2D point);

	// List<? extends Placement2D> getSupports2D();
}
