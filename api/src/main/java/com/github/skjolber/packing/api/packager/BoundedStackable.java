package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.LoadBearingConstraint;
import com.github.skjolber.packing.api.StabilityConstraint;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class BoundedStackable {

	private static final long serialVersionUID = 1L;

	protected final Stackable stackable;
	protected final BoundedStackValue[] values;
	
	protected final long minimumArea;
	protected final long maximumArea;

	public BoundedStackable(Stackable stackable, BoundedStackValue[] stackValues) {
		this.values = stackValues;
		this.stackable = stackable;
		
		this.minimumArea = Stackable.getMinimumArea(stackValues);
		this.maximumArea = Stackable.getMinimumArea(stackValues);
	}

	public Stackable getStackable() {
		return stackable;
	}

	public StackValue getStackValue(int index) {
		return values[index];
	}

}
