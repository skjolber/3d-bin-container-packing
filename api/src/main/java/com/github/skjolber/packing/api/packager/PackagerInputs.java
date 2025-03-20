package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface PackagerInputs {
	
	int size();
	
	ContainerLoadInputs get(int index);

	void remove(int index, int count);
 
}
