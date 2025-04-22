package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public interface FilteredPointsBuilder<B extends FilteredPointsBuilder<B>> {

	B withBoxItem(BoxItem boxItems);
	
	B withPoints(FilteredPoints points);
	
	B withItems(FilteredBoxItems input);
	
	B withContainer(Container container);
	
	B withStack(Stack stack);
	
	FilteredPoints build();

}
