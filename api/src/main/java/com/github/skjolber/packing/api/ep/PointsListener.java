package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.StackPlacement;

/**
 * 
 * Filter for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredBoxItemGroups} instance.
 * 
 */

public interface PointsListener {

	/**
	 * 
	 * Notify point was used
	 * 
	 */
	
	void accepted(Point point, StackPlacement placemen);
	
}
