package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.NoopBoxItemListener;

public class NoopPlacementFilter implements PlacementFilter {

	private static final NoopPlacementFilter INSTANCE = new NoopPlacementFilter(); 

	public static NoopPlacementFilter getInstance() {
		return INSTANCE;
	}
	
	@Override
	public boolean accepts(BoxItem boxItem, Point point) {
		return true;
	}

}
