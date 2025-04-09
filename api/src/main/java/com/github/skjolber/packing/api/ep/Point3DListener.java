package com.github.skjolber.packing.api.ep;

/**
 * 
 * Filter for points which are available for placing boxes.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredPoint3Ds} instance.
 * 
 */

public interface Point3DListener {

	/**
	 * 
	 * Notify point was loaded
	 * 
	 * @param index index as in {@linkplain FilteredPoint3Ds}
	 */
	
	void packedPoint3D(int index);
	
}
