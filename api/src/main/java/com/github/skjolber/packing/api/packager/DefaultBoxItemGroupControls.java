package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultBoxItemGroupControls implements BoxItemGroupControls {

	protected FilteredBoxItemGroups filteredBoxItemGroups;

	public DefaultBoxItemGroupControls(FilteredBoxItemGroups filteredBoxItemGroups) {
		this.filteredBoxItemGroups = filteredBoxItemGroups;
	}
	
	@Override
	public FilteredBoxItemGroups getFilteredBoxItemGroups() {
		return filteredBoxItemGroups;
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
