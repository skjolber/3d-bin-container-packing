package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public abstract class LoaderInputsFilterBuilder<B extends LoaderInputsFilterBuilder<B>> {

	protected Stack stack;
	protected Container container;
	protected LoaderInputs loaderInputs;

	public B withLoaderInputs(LoaderInputs containerLoadInputs) {
		this.loaderInputs = containerLoadInputs;
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

	public abstract LoaderInputsFilter build();
	
	

}
