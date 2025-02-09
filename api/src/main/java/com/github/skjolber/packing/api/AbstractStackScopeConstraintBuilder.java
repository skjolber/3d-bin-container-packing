package com.github.skjolber.packing.api;

import java.util.List;

public abstract class AbstractStackScopeConstraintBuilder<B extends AbstractStackScopeConstraintBuilder<B>>  {

	protected List<Stackable> stackables;
	protected Container container;
	protected ContainerStackValue containerStackValue;
	protected Stack stack;
	
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}

	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}

	public B withContainerStackValue(ContainerStackValue containerStackValue) {
		this.containerStackValue = containerStackValue;
		return (B)this;
	}
	
	public B withStackables(List<Stackable> stackables) {
		this.stackables = stackables;
		return (B)this;
	}
	
	public abstract StackScopeConstraint build();
}
