package com.github.skjolber.packing.api.packager;

/**
 * 
 * The items which are available for load into some particular container.
 * 
 * Works on an underlying {@linkplain ContainerLoadInputs} instance.
 * 
 */

public interface ContainerLoadInputsFilter {

	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param index
	 */
	
	void loaded(int index);
	
}
