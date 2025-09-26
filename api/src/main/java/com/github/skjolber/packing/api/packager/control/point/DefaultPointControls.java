package com.github.skjolber.packing.api.packager.control.point;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.point.PointSource;

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
