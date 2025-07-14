package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItemGroups} instance.
 * 
 */

public interface BoxItemGroupControls {

	FilteredBoxItemGroups getFilteredBoxItemGroups();

	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param group {@linkplain FilteredBoxBoxItemGroupItems}
	 */
	
	void accepted(BoxItemGroup group);

	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param group {@linkplain FilteredBoxBoxItemGroupItems}
	 */

	void declined(BoxItemGroup group);
}
