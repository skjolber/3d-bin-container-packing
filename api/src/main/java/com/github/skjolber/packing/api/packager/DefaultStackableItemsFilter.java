package com.github.skjolber.packing.api.packager;

public class DefaultStackableItemsFilter implements StackableItemsFilter {

	protected final StackableItems loadableItems;

	public DefaultStackableItemsFilter(StackableItems loadableItems) {
		this.loadableItems = loadableItems;
	}

	@Override
	public void loaded(int index) {
		// do nothing
	}

}