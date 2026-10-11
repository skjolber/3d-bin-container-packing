package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

public class ComparatorPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory  {

	protected final PlacementComparator placementComparator;
	protected final BoxItemComparator boxItemComparator;
	
	public ComparatorPlacementControlsBuilderFactory(PlacementComparator placementComparator,
			BoxItemComparator boxItemComparator) {
		super();
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	@Override
	public ComparatorPlacementControlsBuilder createPlacementControlsBuilder() {
		return new ComparatorPlacementControlsBuilder().withBoxItemComparator(boxItemComparator).withPlacementComparator(placementComparator);
	}

}
