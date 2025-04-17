package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * Filter for points which are available for placing boxes.
 * 
 * The filter is expected to maintain a underlying {@linkplain FilteredPoints} instance.
 * 
 */

public interface PlacementFilter {
	
	boolean accepts(BoxItem boxItem, Point point);
	
}
