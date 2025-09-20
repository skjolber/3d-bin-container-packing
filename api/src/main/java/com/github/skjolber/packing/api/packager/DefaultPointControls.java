package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.PointSource;

public class DefaultPointControls implements PointControls {

	protected PointSource filteredPoints;

	public DefaultPointControls(PointSource filteredPoints) {
		this.filteredPoints = filteredPoints;
	}

	@Override
	public PointSource getPoints(BoxItem boxItem) {
		return filteredPoints;
	}

}
