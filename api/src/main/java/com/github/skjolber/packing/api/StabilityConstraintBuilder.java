package com.github.skjolber.packing.api;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public abstract class StabilityConstraintBuilder<B extends StabilityConstraintBuilder<B>> {

	protected Container container;
	protected ContainerStackValue stackValue;
	protected Stack stack;

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
	
	public abstract StabilityConstraint build();

}
