package com.github.skjolber.packing.api.point;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

public interface PointSourceBuilder {

	PointSourceBuilder withBoxItem(RemainingBoxItem boxItems);
	
	PointSourceBuilder withPoints(PointSource points);
	
	PointSourceBuilder withItems(BoxItemSource input);
	
	PointSourceBuilder withContainer(Container container);
	
	PointSourceBuilder withStack(Stack stack);
	
	PointSource build();

}
