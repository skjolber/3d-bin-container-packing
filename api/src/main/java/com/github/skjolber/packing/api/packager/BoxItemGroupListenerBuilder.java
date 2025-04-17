package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class BoxItemGroupListenerBuilder<B extends BoxItemGroupListenerBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItemGroups groups;

	public B withFilteredBoxItemGroups(FilteredBoxItemGroups input) {
		this.groups = input;
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
	
	public abstract BoxItemGroupListener build();

}
