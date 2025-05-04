package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItemGroups} instance.
 * 
 */

public interface BoxItemGroupControls extends BoxItemControls {

	void attempt(BoxItemGroup group);
	
	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param group index as in {@linkplain FilteredBoxItems}
	 */
	
	void accepted(BoxItemGroup group);
	
	void declined(BoxItemGroup group);
}
