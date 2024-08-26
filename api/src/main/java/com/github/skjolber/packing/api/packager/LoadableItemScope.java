package com.github.skjolber.packing.api.packager;

import java.util.List;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface LoadableItemScope {

	List<LoadableItem> getLoadableItems();

	/**
	 * 
	 * Notify item was loaded
	 * 
	 * @param index
	 * @return true if some loadable item was excluded due to this
	 */
	
	boolean loaded(int index);
}
