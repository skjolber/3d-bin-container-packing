package com.github.skjolber.packing.api;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

/**
 * 
 * A {@linkplain Stackable} in a specific {@linkplain StackValue}.
 *
 */

public class ContainerLoadStackValue {

	private final Stackable stackable;
	private final StackValue value;

	public ContainerLoadStackValue(Stackable stackable, StackValue value) {
		super();
		this.stackable = stackable;
		this.value = value;
	}

	public Stackable getStackable() {
		return stackable;
	}

	public StackValue getValue() {
		return value;
	}

}
