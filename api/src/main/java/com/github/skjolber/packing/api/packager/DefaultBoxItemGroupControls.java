package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ep.FilteredPoints;

public class DefaultBoxItemGroupControls implements BoxItemGroupControls {

	protected DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems();
	protected FilteredPoints filteredPoints;
	
	public DefaultBoxItemGroupControls(FilteredPoints filteredPoints) {
		this.filteredPoints = filteredPoints;
	}

	@Override
	public FilteredBoxItems getFilteredBoxItems() {
		return filteredBoxItems;
	}

	@Override
	public FilteredPoints getPoints(BoxItem boxItem) {
		return filteredPoints;
	}

	@Override
	public void attempt(BoxItemGroup group) {
		filteredBoxItems.setValues(group.getItems());
	}

	@Override
	public void accepted(BoxItemGroup group) {
		// do nothing
	}

	@Override
	public void declined(BoxItemGroup group) {
		// do nothing
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
	}

}
