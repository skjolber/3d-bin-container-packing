package com.github.skjolber.packing.api;

import java.util.List;

public interface ExtremePoints<P extends Placement2D, Point extends Point2D<P>> {

	boolean add(int index, P placement);

	Point getValue(int i);
	
	List<Point> getValues();

	void reset(int dx, int dy, int dz);
	
	void redo();
}
