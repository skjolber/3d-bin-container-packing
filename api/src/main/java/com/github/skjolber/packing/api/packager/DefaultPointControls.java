package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

public class DefaultPointControls implements PointControls {

	protected FilteredPoints filteredPoints;
	
	public DefaultPointControls(FilteredPoints filteredPoints) {
		this.filteredPoints = filteredPoints;
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
	}
	
	@Override
	public void declined(BoxItem boxItem) {
		// do nothing
	}

	@Override
	public FilteredPoints getFilteredPoints(BoxItem boxItem) {
		return filteredPoints;
	}

}
