package com.github.skjolber.packing.api;

public class SupportedStackPlacement extends StackPlacement {

	private static final long serialVersionUID = 1L;

	public SupportedStackPlacement() {
		super();
	}

	public SupportedStackPlacement(Stackable stackable, StackValue value, int x, int y, int z) {
		super(stackable, value, x, y, z);
	}

	
}
