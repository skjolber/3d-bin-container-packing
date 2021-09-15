package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.points.Placement2D;

public interface Placement3D extends Placement2D  {

	int getAbsoluteZ();
	
	int getAbsoluteEndZ();
	
	boolean intersects(Placement3D point);

}
