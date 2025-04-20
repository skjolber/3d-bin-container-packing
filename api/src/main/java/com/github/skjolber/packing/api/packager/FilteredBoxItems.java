package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItems {
	
	int size();

	boolean isEmpty();

	BoxItem get(int index);

	BoxItem decrement(int index, int count);
 
	BoxItem remove(int index);

	void clearEmpty();
}
