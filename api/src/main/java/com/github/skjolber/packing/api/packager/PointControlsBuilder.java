package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items vs container and possibly in-flight box vs box filtering.
 */

public interface PointControlsBuilder<B extends PointControlsBuilder<B>> {

	B withPoints(FilteredPoints points);
	
	B withBoxItems(FilteredBoxItems input);
	
	B withContainer(Container container);
	
	B withStack(Stack stack);
	
	PointControls build();

}
