package com.github.skjolber.packing.api.ep;

import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;

public interface ExtremePoints extends FilteredPoints {

	boolean add(int index, StackPlacement placement);

	List<Point> getValues();

	void reset(int dx, int dy, int dz);

	void redo();
	
	List<StackPlacement> getPlacements();

	long getUsedVolume();

	long getUsedWeight();
}
