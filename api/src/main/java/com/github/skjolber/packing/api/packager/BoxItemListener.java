package com.github.skjolber.packing.api.packager;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItems} instance.
 * 
 */

public interface BoxItemListener {

	/**
	 * 
	 * Notify box was loaded
	 * 
	 * @param index index as in {@linkplain FilteredBoxItems}
	 */
	
	void packed(int index);
	
}
