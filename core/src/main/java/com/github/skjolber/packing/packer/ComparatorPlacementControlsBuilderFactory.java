package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

public class ComparatorPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<Placement>  {

	protected final Comparator<Placement> placementComparator;
	protected final Comparator<BoxItem> boxItemComparator;
	
	public ComparatorPlacementControlsBuilderFactory(Comparator<Placement> placementComparator,
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
