package com.github.skjolber.packing.api.ep;

import java.math.BigDecimal;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public interface ExtremePoints<P extends Placement2D, Point extends Point2D<P>> {

	boolean add(int index, P placement);

	Point getValue(int i);

	List<Point> getValues();

	int getValueCount();

	void reset(BigDecimal dx, BigDecimal dy, BigDecimal dz);

	void redo();
}
