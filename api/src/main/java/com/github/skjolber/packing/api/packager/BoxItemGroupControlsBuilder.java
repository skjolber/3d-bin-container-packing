package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public interface BoxItemGroupControlsBuilder<B extends BoxItemGroupControlsBuilder<B>> {

	B withPoints(FilteredPoints points);
	
	B withFilteredBoxItemGroups(FilteredBoxItemGroups input);
	
	B withContainer(Container container);
	
	B withStack(Stack stack);
	
	BoxItemGroupControls build();

}
