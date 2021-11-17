package com.github.skjolber.packing.points2d;

import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public interface ExtremePoints<P extends Placement2D, Point extends Point2D> {

	boolean add(int index, P placement);

	Point getValue(int i);
	
	List<Point> getValues();

	void reset(int dx, int dy, int dz);
	
	void reset();
}
