package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Controls (filter) for items and points which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItems} instance.
 * 
 */

public interface BoxItemControls {

	FilteredBoxItems getFilteredBoxItems();
	FilteredPoints getPoints(BoxItem boxItem);

	/**
	 * 
	 * Notify group was loaded
	 * 
	 * @param boxItem index as in {@linkplain FilteredBoxItemGroups}
	 */
	
	void accepted(BoxItem boxItem);
	
}
