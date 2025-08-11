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
public abstract class AbstractPointControlsBuilder<B extends AbstractPointControlsBuilder<B>> implements PointControlsBuilder<B> {

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItems items;
	protected FilteredPoints points;
	protected FilteredBoxItemGroups groups;

	public B withBoxItemGroups(FilteredBoxItemGroups groups) {
		this.groups = groups;
		return (B)this;
	}
	
	public B withPoints(FilteredPoints points) {
		this.points = points;
		return (B)this;
	}

	public B withBoxItems(FilteredBoxItems input) {
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
	
	public abstract PointControls build();

}
