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


	/**
	 * 
	 * Notify box group was loaded. 
	 * 
	 * @param group {@linkplain BoxItemGroup} to be added.
	 */
	
	default void attempt(BoxItemGroup group, int offset, int length) {
		
	}
	
	/**
	 * 
	 * Notify box cannot be fitted.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 */
	
	default void accepted(BoxItemGroup group) {		
	}

	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 */
	
	default void declined(BoxItemGroup group) {
	}
}
