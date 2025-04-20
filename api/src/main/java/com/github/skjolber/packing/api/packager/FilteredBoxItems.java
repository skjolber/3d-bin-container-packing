package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItems<T extends BoxItem> {
	
	int size();

	boolean isEmpty();

	T get(int index);

	T decrement(int index, int count);
 
	T remove(int index);

	void clearEmpty();
}
