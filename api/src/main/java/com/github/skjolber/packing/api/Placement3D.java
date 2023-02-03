package com.github.skjolber.packing.api;

public interface Placement3D extends Placement2D {

	int getAbsoluteZ();

	int getAbsoluteEndZ();

	boolean intersects3D(Placement3D point);

	// List<? extends Placement3D> getSupports3D();

}
