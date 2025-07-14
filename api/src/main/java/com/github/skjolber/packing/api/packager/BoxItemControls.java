package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Controls (filter) for items and points which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain FilteredBoxItems} and {@linkplain FilteredPoints} instances.
 * 
 */

public interface BoxItemControls {

	FilteredBoxItems getFilteredBoxItems();
	FilteredPoints getFilteredPoints(BoxItem boxItem);

	/**
	 * 
	 * Notify box was loaded. 
	 * 
	 * @param boxItem {@linkplain BoxItem} to be added.
	 */
	
	void accepted(BoxItem boxItem);
	
	/**
	 * 
	 * Notify box cannot be fitted.
	 * 
	 * @param group {@linkplain FilteredBoxBoxItemGroupItems}
	 */
	
	void declined(BoxItem group);

}
