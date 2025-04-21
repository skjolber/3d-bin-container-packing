package com.github.skjolber.packing.api.packager;

import java.util.function.Supplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class BoxItemListenerBuilder<B extends BoxItemListenerBuilder<B>> {

	@FunctionalInterface
	public static interface BoxItemListenerBuilderSupplier<B extends BoxItemListenerBuilder<B>> extends Supplier<BoxItemListenerBuilder<B>> {
		
	}

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItems items;
	protected FilteredPoints points;

	public B withPoints(FilteredPoints points) {
		this.points = points;
		return (B)this;
	}

	public B withFilteredBoxItems(FilteredBoxItems input) {
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
	
	public abstract BoxItemListener build();
	
	

}
