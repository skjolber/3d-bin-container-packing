package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * The items for a specific container stacking session.
 * 
 */

public interface PackInput {
	
	int size();
	
	BoxItem get(int index);

	void remove(int index, int count);
 
}
