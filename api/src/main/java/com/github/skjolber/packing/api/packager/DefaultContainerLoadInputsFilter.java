package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * All items can be loaded and no dependencies across stackable items exist.
 * 
 * @param <S>
 */

public class DefaultContainerLoadInputsFilter<S extends BoxItem> implements ContainerLoadInputsFilter {

	protected final ContainerLoadInputs<S> loadableItems;

	public DefaultContainerLoadInputsFilter(ContainerLoadInputs<S> loadableItems) {
		this.loadableItems = loadableItems;
	}

	@Override
	public void loaded(int index) {
		// do nothing
	}

}