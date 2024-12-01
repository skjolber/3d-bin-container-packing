package com.github.skjolber.packing.api.packager;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 */

public interface ContainerLoadInputsFilter {

	/**
	 * 
	 * Notify stackable was loaded
	 * 
	 * @param index
	 */
	
	void loaded(int index);
	
}
