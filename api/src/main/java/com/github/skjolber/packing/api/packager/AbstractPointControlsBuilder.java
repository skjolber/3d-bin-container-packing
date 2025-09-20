package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPointControlsBuilder<B extends AbstractPointControlsBuilder<B>> implements PointControlsBuilder<B> {

	protected Stack stack;
	protected Container container;
	protected BoxItemSource items;
	protected PointSource points;
	protected BoxItemGroupSource groups;

	public B withBoxItemGroups(BoxItemGroupSource groups) {
		this.groups = groups;
		return (B)this;
	}
	
	public B withPoints(PointSource points) {
		this.points = points;
		return (B)this;
	}

	public B withBoxItems(BoxItemSource input) {
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
