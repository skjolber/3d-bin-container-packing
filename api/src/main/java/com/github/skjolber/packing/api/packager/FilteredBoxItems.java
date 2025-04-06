package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItems {
	
	int size();
	
	BoxItem get(int index);

	void remove(int index, int count);
 
}
