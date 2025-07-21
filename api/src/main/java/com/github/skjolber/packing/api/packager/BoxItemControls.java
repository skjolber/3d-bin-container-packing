package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Controls (filter) for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain FilteredBoxItems} and {@linkplain FilteredPoints} instances.
 * 
 */

public interface BoxItemControls {

	/**
	 * 
	 * Notify box was loaded. 
	 * 
	 * @param boxItem {@linkplain BoxItem} to be added.
	 */
	
	default void accepted(BoxItem boxItem) {
		
	}
	
	/**
	 * 
	 * Notify box cannot be fitted.
	 * 
	 * @param boxItem {@linkplain FilteredBoxBoxItemGroupItems}
	 */
	
	default void declined(BoxItem boxItem) {		
	}

	/**
	 * 
	 * Notify box cannot be fitted, even it was previously accepted; usually because
	 * fitting the whole group was not possible.
	 * 
	 * @param boxItem {@linkplain BoxItem}
	 */
	
	default void undo(BoxItem boxItem) {
	}




}
