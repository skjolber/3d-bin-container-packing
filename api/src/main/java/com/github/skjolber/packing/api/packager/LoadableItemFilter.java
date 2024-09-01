package com.github.skjolber.packing.api.packager;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface LoadableItemFilter {

	/**
	 * 
	 * Notify loadable was loaded
	 * 
	 * @param index
	 * @return true if some loadable item was excluded due to this loaded item
	 */
	
	void loaded(int index);
	
}
