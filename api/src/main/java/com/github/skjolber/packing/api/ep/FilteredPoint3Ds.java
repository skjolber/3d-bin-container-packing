package com.github.skjolber.packing.api.ep;

/**
 * 
 * Filter which determines which points are acceptable to use.
 * 
 */

public interface FilteredPoint3Ds {

	int size();
	
	Point3D get(int index);

	void remove(int index);

}