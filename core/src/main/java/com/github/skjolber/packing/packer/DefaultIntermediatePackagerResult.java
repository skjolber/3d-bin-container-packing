package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Stack;

public class DefaultIntermediatePackagerResult implements IntermediatePackagerResult {

	protected ControlledContainerItem containerItem;
	protected Stack stack;
	
	public DefaultIntermediatePackagerResult(ControlledContainerItem containerItem, Stack stack) {
		this.containerItem = containerItem;
		this.stack = stack;
	}

	@Override
	public ControlledContainerItem getContainerItem() {
		return containerItem;
	}
	
	@Override
	public Stack getStack() {
		return stack;
	}

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

}
