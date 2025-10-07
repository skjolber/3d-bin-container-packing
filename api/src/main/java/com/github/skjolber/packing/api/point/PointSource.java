package com.github.skjolber.packing.api.point;

import java.util.List;

/**
 * 
 * Filter which determines which points are acceptable to use.
 * 
 */

public interface PointSource extends Iterable<Point> {

	int size();
	
	Point get(int index);

	void remove(int index);
	
	List<Point> getAll();

	default long getMaxVolume() {
		long maxPointVolume = -1L;
		for (int i = 0; i < size(); i++) {
			Point point = get(i);
			if(maxPointVolume < point.getVolume()) {
				maxPointVolume = point.getVolume();
			}
		}
		return maxPointVolume;
	}
	
	default long getMaxArea() {
		long maxPointArea = -1L;
		for (int i = 0; i < size(); i++) {
			Point point = get(i);
			if(maxPointArea < point.getArea()) {
				maxPointArea = point.getArea();
			}
		}
		return maxPointArea;
	}

}