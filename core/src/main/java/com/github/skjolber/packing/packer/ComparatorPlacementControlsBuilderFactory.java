package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

public class ComparatorPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory  {

	protected final PlacementComparator placementComparator;
	protected final Comparator<BoxItem> boxItemComparator;
	
	public ComparatorPlacementControlsBuilderFactory(PlacementComparator placementComparator,
			Comparator<BoxItem> boxItemComparator) {
		super();
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	@Override
	public ComparatorPlacementControlsBuilder createPlacementControlsBuilder() {
		return new ComparatorPlacementControlsBuilder().withBoxItemComparator(boxItemComparator).withPlacementComparator(placementComparator);
	}

}
