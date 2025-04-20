package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItemGroups<T extends BoxItemGroup> {
	
	int size();
	
	T get(int index);

	T remove(int index);
 
	boolean isEmpty();

	void clearEmpty();
}
