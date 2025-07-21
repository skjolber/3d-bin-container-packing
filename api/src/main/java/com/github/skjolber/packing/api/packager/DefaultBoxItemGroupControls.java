package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultBoxItemGroupControls implements BoxItemGroupControls {

	protected FilteredBoxItems filteredBoxItems;
	
	public DefaultBoxItemGroupControls(FilteredBoxItems filteredBoxItems) {
		this.filteredBoxItems = filteredBoxItems;
	}
	
	@Override
	public void accepted(BoxItemGroup group) {
		// do nothing
	}

	@Override
	public void declined(BoxItemGroup group) {
		// do nothing
	}

}
