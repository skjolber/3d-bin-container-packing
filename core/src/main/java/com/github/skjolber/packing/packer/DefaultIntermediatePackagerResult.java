package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public class DefaultIntermediatePackagerResult implements IntermediatePackagerResult {

	protected ContainerItem containerItem;
	protected Stack stack;
	
	public DefaultIntermediatePackagerResult(ContainerItem containerItem, Stack stack) {
		this.containerItem = containerItem;
		this.stack = stack;
	}

	@Override
	public ContainerItem getContainerItem() {
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
