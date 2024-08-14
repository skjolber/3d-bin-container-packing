package com.github.skjolber.packing.api;

import java.util.List;

public class DefaultStackScopeConstraint implements StackScopeConstraint {

	protected final List<Stackable> stackables;
	protected final Container container;
	protected final ContainerStackValue containerStackValue;
	protected final Stack stack;
	
	public DefaultStackScopeConstraint(List<Stackable> stackables, Container container, ContainerStackValue containerStackValue, Stack stack) {
		this.stackables = stackables;
		this.container = container;
		this.containerStackValue = containerStackValue;
		this.stack = stack;
	}
	
	@Override
	public List<Stackable> getStackScope() {
		return stackables;
	}

	@Override
	public void stacked(int index) {
		stackables.remove(index);
	}

}
