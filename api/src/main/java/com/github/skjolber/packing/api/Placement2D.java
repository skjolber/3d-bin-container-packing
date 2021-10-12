package com.github.skjolber.packing.api;

public interface Placement2D {

	int getAbsoluteX();
	
	int getAbsoluteY();

	int getAbsoluteEndX();
	
	int getAbsoluteEndY();
	
	boolean intersects(Placement2D point);
}
