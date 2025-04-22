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
public abstract class AbstractFilteredPointsBuilder<B extends AbstractFilteredPointsBuilder<B>> implements FilteredPointsBuilder<B> {

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItems items;
	protected FilteredPoints points;
	protected BoxItem boxItem;

	public B withBoxItem(BoxItem boxItems) {
		this.boxItem = boxItems;
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
