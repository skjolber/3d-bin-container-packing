package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Listener for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain FilteredBoxItems} and {@linkplain FilteredPoints} instances.
 * 
 */

public interface ManifestListener {

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
	
	default void declined(List<BoxItem> boxItems) {		
	}

	/**
	 * 
	 * Notify box cannot be fitted, even it was previously accepted; usually because
	 * fitting the whole group was not possible.
	 * 
	 * @param boxItem {@linkplain BoxItem}
	 */
	
	default void undo(List<BoxItem> boxItems) {
	}




}
