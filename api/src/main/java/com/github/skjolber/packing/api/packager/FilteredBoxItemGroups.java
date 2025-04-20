package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * List of box item which have been filtered.
 * 
 */

public interface FilteredBoxItemGroups {
	
	int size();
	
	BoxItemGroup get(int index);

	BoxItemGroup remove(int index);
 
	boolean isEmpty();

	void clearEmpty();
}
