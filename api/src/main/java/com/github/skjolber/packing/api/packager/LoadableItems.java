package com.github.skjolber.packing.api.packager;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface LoadableItems {
	
	int size();
	
	LoadableItem get(int index);

	void remove(int index, int count);
 
}
