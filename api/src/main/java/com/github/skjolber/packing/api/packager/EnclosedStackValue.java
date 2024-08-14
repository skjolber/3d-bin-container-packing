package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

/**
 * 
 * A {@linkplain Stackable} in a specific {@linkplain StackValue} with certain dimensions.
 *
 */

public class EnclosedStackValue {

	private final Stackable stackable;
	private final StackValue value;

	public EnclosedStackValue(Stackable stackable, StackValue value) {
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
