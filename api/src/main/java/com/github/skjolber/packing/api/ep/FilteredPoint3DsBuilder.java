package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class FilteredPoint3DsBuilder<B extends FilteredPoint3DsBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected FilteredBoxItems input;
	protected ExtremePoints extremePoints;

	public B withExtremePoints(ExtremePoints extremePoints) {
		this.extremePoints = extremePoints; 
		return (B) this;
	}
	
	public B withLoaderInputs(FilteredBoxItems input) {
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
	
	public abstract FilteredPoint3Ds build();

}
