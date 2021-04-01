package com.github.skjolber.packing.api.impl;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class StackableValue {

	private final Stackable stackable;
	private final StackValue value;
	
	public StackableValue(Stackable stackable, StackValue value) {
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
