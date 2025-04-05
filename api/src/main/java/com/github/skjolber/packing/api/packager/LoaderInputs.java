package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * The items which are available for a loading session.
 * 
 */

public interface LoaderInputs {
	
	int size();
	
	BoxItem get(int index);

	void remove(int index, int count);
 
}
