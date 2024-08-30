package com.github.skjolber.packing.api.packager;

import java.util.List;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface LoadableItemFilter {

	List<LoadableItem> getLoadableItems();

	/**
	 * 
	 * Notify stackable from item was loaded
	 * 
	 * @param index
	 * @return true if some loadable item was excluded due to this loaded item
	 */
	
	boolean loaded(int index);
}
