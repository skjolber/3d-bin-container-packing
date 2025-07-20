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

	FilteredBoxItems getFilteredBoxItems();

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

	/**
	 * 
	 * Notify start of group.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 */

	default void begin(BoxItemGroup group) {	
	}

	/**
	 * 
	 * Notify end of group.
	 * 
	 * Corresponding calls to declined and/or undo will be performed before this call.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 * @param accepted true if all items could fit.
	 */

	default void ended(BoxItemGroup group, boolean accepted) {		
	}

}
