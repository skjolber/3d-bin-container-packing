package com.github.skjolber.packing.api.ep;

/**
 * 
 * Filter for points which are available for placing boxes.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredPoints} instance.
 * 
 */

public interface PointListener {

	/**
	 * 
	 * Notify point was loaded
	 * 
	 * @param index index as in {@linkplain FilteredPoints}
	 */
	
	void packedPoint3D(int index);
	
}
