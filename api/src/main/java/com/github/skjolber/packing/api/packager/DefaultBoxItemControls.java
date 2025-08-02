package com.github.skjolber.packing.api.packager;

public class DefaultBoxItemControls implements BoxItemControls {

	protected FilteredBoxItems filteredBoxItems;
	
	public DefaultBoxItemControls(FilteredBoxItems filteredBoxItems) {
		this.filteredBoxItems = filteredBoxItems;
	}

}
