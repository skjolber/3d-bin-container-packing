package com.github.skjolber.packing.api.packager;

import java.util.List;

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
public abstract class LoadableItemFilterBuilder<B extends LoadableItemFilterBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected ContainerStackValue stackValue;
	protected LoadableItems loadableItems;

	public B withLoadableItems(LoadableItems loadableItems) {
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
	
	public abstract LoadableItemFilter build();
	
	

}
