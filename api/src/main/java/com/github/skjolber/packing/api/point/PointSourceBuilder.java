package com.github.skjolber.packing.api.point;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

public interface PointSourceBuilder {

	PointSourceBuilder withBoxItem(BoxItem boxItems);
	
	PointSourceBuilder withPoints(PointSource points);
	
	PointSourceBuilder withItems(BoxItemSource input);
	
	PointSourceBuilder withContainer(Container container);
	
	PointSourceBuilder withStack(Stack stack);
	
	PointSource build();

}
