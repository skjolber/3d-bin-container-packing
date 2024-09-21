package com.github.skjolber.packing.api.packager;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface StackableItemsFilter {

	/**
	 * 
	 * Notify stackable was loaded
	 * 
	 * @param index
	 * @return true if some loadable item was excluded due to this loaded item
	 */
	
	void loaded(int index);
	
}
