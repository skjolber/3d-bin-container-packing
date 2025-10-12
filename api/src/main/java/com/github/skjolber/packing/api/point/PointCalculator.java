package com.github.skjolber.packing.api.point;

import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Placement;

public interface PointCalculator extends PointSource {

	boolean add(Point point, Placement placement);
	
	boolean add(int index, Placement placement);

	void clearToSize(int dx, int dy, int dz);
	void clear();
	
	List<Placement> getPlacements();

	long calculateUsedVolume();
	long calculateUsedWeight();
	
	void setPoints(List<Point> points);
	
	void setMinimumAreaAndVolumeLimit(long area, long volume);
	
	void remove(Predicate<Point> test);

}
