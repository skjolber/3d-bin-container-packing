package com.github.skjolber.packing.api;

public class StackCandidate {

	private final Stackable stackable;
	private final StackValue value;
	
	public StackCandidate(Stackable stackable, StackValue value) {
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
