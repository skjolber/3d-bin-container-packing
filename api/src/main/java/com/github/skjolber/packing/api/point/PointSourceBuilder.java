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

@SuppressWarnings("unchecked")
public interface PointSourceBuilder<B extends PointSourceBuilder<B>> {

	B withBoxItem(BoxItem boxItems);
	
	B withPoints(PointSource points);
	
	B withItems(BoxItemSource input);
	
	B withContainer(Container container);
	
	B withStack(Stack stack);
	
	PointSource build();

}
