package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class PlacementFilterBuilder<B extends PlacementFilterBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected ExtremePoints input;

	public B withExtremePoints(ExtremePoints input) {
		this.input = input;
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
	
	public abstract PlacementFilter build();
	
	

}
