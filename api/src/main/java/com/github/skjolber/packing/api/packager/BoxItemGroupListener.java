package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItems} instance.
 * 
 */

public interface BoxItemGroupListener {

	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param group index as in {@linkplain FilteredBoxItems}
	 */
	
	void accepted(BoxItemGroup group);
	
}
