package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.StackableItem;

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
