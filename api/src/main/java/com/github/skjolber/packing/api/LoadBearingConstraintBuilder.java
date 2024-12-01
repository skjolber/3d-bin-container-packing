package com.github.skjolber.packing.api;

/**
 * 
 * Load bearing constraint for a stacked {@linkplain Stackable}.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class LoadBearingConstraintBuilder<B extends LoadBearingConstraintBuilder<B>> {

	protected Container container;
	protected ContainerStackValue stackValue;
	protected Stack stack;
	protected StackPlacement stackPlacement;

	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	public B withStackValue(ContainerStackValue stackValue) {
		this.stackValue = stackValue;
		return (B)this;
	}
	
	public B withStackPlacement(StackPlacement stackPlacement) {
		this.stackPlacement = stackPlacement;
		return (B)this;
	}
	
	public abstract LoadBearingConstraint build();
	
	

}
