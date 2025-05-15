package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackableItem;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public abstract class ContainerLoadInputsFilterBuilder<S extends StackableItem, B extends ContainerLoadInputsFilterBuilder<S, B>> {

	protected Stack stack;
	protected Container container;
	protected ContainerStackValue stackValue;
	protected ContainerLoadInputs<S> stackableItemCollection;

	public B withStackableItems(ContainerLoadInputs<S> stackableItemCollection) {
		this.stackableItemCollection = stackableItemCollection;
		return (B)this;
	}
	
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
	
	public abstract ContainerLoadInputsFilter build();
	
	

}
