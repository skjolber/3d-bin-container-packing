package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Stack;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public abstract class StackableItemsFilterBuilder<B extends StackableItemsFilterBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected ContainerStackValue stackValue;
	protected StackableItems loadableItems;

	public B withLoadableItems(StackableItems loadableItems) {
		this.loadableItems = loadableItems;
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
	
	public abstract StackableItemsFilter build();
	
	

}
