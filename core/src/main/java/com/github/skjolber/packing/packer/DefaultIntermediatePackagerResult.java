package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public class DefaultIntermediatePackagerResult implements IntermediatePackagerResult {

	protected ContainerItem containerItem;
	protected Stack stack;
	protected boolean containsLastStackable;
	
	public DefaultIntermediatePackagerResult(ContainerItem containerItem, Stack stack, boolean containsLastStackable) {
		super();
		this.containerItem = containerItem;
		this.stack = stack;
		this.containsLastStackable = containsLastStackable;
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
	public boolean containsLastStackable() {
		return containsLastStackable;
	}
	
	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

}
