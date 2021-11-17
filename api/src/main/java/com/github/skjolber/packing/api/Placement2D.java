package com.github.skjolber.packing.api;

import java.util.List;

public interface Placement2D {

	int getAbsoluteX();
	
	int getAbsoluteY();

	int getAbsoluteEndX();
	
	int getAbsoluteEndY();
	
	boolean intersects2D(Placement2D point);
	
	List<? extends Placement2D> getSupports2D();
}
