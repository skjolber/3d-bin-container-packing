package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

public class DefaultBoxItemControls implements BoxItemControls {

	protected FilteredBoxItems filteredBoxItems;
	protected FilteredPoints filteredPoints;
	
	public DefaultBoxItemControls(FilteredBoxItems filteredBoxItems, FilteredPoints filteredPoints) {
		this.filteredBoxItems = filteredBoxItems;
		this.filteredPoints = filteredPoints;
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
	}

	@Override
	public FilteredBoxItems getFilteredBoxItems() {
		return filteredBoxItems;
	}

	@Override
	public FilteredPoints getPoints(BoxItem boxItem) {
		return filteredPoints;
	}

}
