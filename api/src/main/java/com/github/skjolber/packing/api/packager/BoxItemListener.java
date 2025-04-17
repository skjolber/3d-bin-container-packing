package com.github.skjolber.packing.api.packager;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItemGroups} instance.
 * 
 */

public interface BoxItemListener {

	/**
	 * 
	 * Notify group was loaded
	 * 
	 * @param index index as in {@linkplain FilteredBoxItemGroups}
	 */
	
	void packedBoxItem(int index);
	
}
