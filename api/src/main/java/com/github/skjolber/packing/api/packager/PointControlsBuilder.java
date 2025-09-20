package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers filtering of points per placement and/or per box item.
 */

public interface PointControlsBuilder<B extends PointControlsBuilder<B>> {

	B withPoints(PointSource points);
	
	B withBoxItems(BoxItemSource boxItems);
	
	B withContainer(Container container);
	
	B withStack(Stack stack);
	
	PointControls build();

}
