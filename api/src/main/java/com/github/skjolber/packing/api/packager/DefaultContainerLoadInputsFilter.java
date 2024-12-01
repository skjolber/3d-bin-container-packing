package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.StackableItem;

public class DefaultContainerLoadInputsFilter<S extends StackableItem> implements ContainerLoadInputsFilter {

	protected final ContainerLoadInputs<S> loadableItems;

	public DefaultContainerLoadInputsFilter(ContainerLoadInputs<S> loadableItems) {
		this.loadableItems = loadableItems;
	}

	@Override
	public void loaded(int index) {
		// do nothing
	}

}