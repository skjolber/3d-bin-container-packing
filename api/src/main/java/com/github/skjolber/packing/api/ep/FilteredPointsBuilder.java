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
public abstract class FilteredPointsBuilder<B extends FilteredPointsBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItems items;
	protected FilteredPoints points;
	protected BoxItem boxItems;

	public B withBoxItems(BoxItem boxItems) {
		this.boxItems = boxItems;
		return (B) this;
	}
	
	public B withPoints(FilteredPoints points) {
		this.points = points; 
		return (B) this;
	}
	
	public B withItems(FilteredBoxItems input) {
		this.items = input;
		return (B)this;
	}
	
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	public abstract FilteredPoints build();

}
