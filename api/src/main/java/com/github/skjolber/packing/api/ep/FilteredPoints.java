package com.github.skjolber.packing.api.ep;

/**
 * 
 * Filter which determines which points are acceptable to use.
 * 
 */

public interface FilteredPoints {

	int size();
	
	Point get(int index);

	void remove(int index);

}