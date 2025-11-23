package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public class EmptyPackagerResultAdapter implements IntermediatePackagerResult {

	public static final EmptyPackagerResultAdapter EMPTY = new EmptyPackagerResultAdapter();
	
	// empty stack
	private final Stack stack = new Stack();
	
	@Override
	public ControlledContainerItem getContainerItem() {
		return null;
	}

	@Override
	public Stack getStack() {
		return stack;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

}
