package com.github.skjolber.packing.api.packager;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain LoaderInputs} instance.
 * 
 */

public interface LoaderInputsFilter {

	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param index
	 */
	
	void loaded(int index);
	
}
