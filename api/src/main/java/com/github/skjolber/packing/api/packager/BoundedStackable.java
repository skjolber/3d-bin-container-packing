package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class BoundedStackable extends Stackable {

	private static final long serialVersionUID = 1L;
	
	protected final StackValue[] values;
	protected final Stackable stackable;
	
	protected final long minimumArea;
	protected final long maximumArea;

	public BoundedStackable(Stackable stackable, StackValue[] stackValues) {
		this.values = stackValues;
		this.stackable = stackable;

		this.minimumArea = getMinimumArea(stackValues);
		this.maximumArea = getMinimumArea(stackValues);
	}

	public BoundedStackable(Stackable stackable, List<StackValue> stackValues) {
		this(stackable, stackValues.toArray(new StackValue[stackValues.size()]));
	}

	public Stackable getStackable() {
		return stackable;
	}

	public StackValue getStackValue(int index) {
		return values[index];
	}

	@Override
	public long getVolume() {
		return stackable.getVolume();
	}

	@Override
	public int getWeight() {
		return stackable.getWeight();
	}

	@Override
	public StackValue[] getStackValues() {
		return values;
	}

	@Override
	public String getDescription() {
		return stackable.getDescription();
	}

	@Override
	public String getId() {
		return stackable.getId();
	}

	@Override
	public Stackable clone() {
		return new BoundedStackable(stackable.clone(), values);
	}

	@Override
	public long getMinimumArea() {
		return minimumArea;
	}

	@Override
	public long getMaximumArea() {
		return maximumArea;
	}
}
