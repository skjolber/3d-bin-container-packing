package com.github.skjolber.packing.api;

public interface Placement3D extends Placement2D  {

	int getAbsoluteZ();
	
	int getAbsoluteEndZ();
	
	boolean intersects(Placement3D point);

	Placement3D rotate();
}
