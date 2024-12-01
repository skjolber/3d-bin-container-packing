package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.StackableItem;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface ContainerLoadInputs<S extends StackableItem> {
	
	int size();
	
	S get(int index);

	void remove(int index, int count);
 
}
