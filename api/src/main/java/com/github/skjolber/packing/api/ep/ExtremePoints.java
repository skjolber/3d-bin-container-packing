package com.github.skjolber.packing.api.ep;

import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.StackPlacement;

public interface ExtremePoints extends FilteredPoints {

	boolean add(Point point, StackPlacement placement);
	
	boolean add(int index, StackPlacement placement);

	List<Point> getAll();

	void clearToSize(int dx, int dy, int dz);

	void clear();
	
	List<StackPlacement> getPlacements();

	long calculateUsedVolume();

	long calculateUsedWeight();
	
	void setPoints(List<Point> points);
	
	void setMinimumAreaAndVolumeLimit(long area, long volume);
	
	void remove(Predicate<Point> test);
}
