package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultBoxItemControls implements BoxItemControls {

	protected FilteredBoxItems filteredBoxItems;
	
	public DefaultBoxItemControls(FilteredBoxItems filteredBoxItems) {
		this.filteredBoxItems = filteredBoxItems;
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
	}

	@Override
	public void declined(BoxItem boxItem) {
		
	}

	@Override
	public FilteredBoxItems getFilteredBoxItems() {
		return filteredBoxItems;
	}


}
