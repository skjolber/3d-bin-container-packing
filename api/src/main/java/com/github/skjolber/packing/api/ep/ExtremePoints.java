package com.github.skjolber.packing.api.ep;

import java.io.Serializable;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public interface ExtremePoints<P extends Placement2D & Serializable, Point extends Point2D<P>> {

	boolean add(int index, P placement);

	Point getValue(int i);

	List<Point> getValues();

	int getValueCount();

	void reset(int dx, int dy, int dz);

	void redo();
}
