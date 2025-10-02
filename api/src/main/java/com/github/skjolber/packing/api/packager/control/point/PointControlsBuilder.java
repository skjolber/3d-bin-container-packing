package com.github.skjolber.packing.api.packager.control.point;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers filtering of points per placement and/or per box item.
 */

public interface PointControlsBuilder {

	PointControlsBuilder withPoints(PointSource points);
	
	PointControlsBuilder withBoxItems(BoxItemSource boxItems);
	
	PointControlsBuilder withContainer(Container container);
	
	PointControlsBuilder withStack(Stack stack);
	
	PointControls build();

}
