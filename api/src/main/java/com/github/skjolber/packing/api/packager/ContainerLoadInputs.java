package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface ContainerLoadInputs {
	
	int size();
	
	BoxItem get(int index);

	void remove(int index, int count);
 
}
