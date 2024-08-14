package com.github.skjolber.packing.api.ep;

import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;

public interface ExtremePoints {

	boolean add(int index, StackPlacement placement);

	Point3D getValue(int i);

	List<Point3D> getValues();

	int getValueCount();

	void reset(int dx, int dy, int dz);

	void redo();
	
	List<StackPlacement> getPlacements();
}
