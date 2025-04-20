package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

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
	 * @param boxItem index as in {@linkplain FilteredBoxItemGroups}
	 */
	
	void accepted(BoxItem boxItem);
	
}
