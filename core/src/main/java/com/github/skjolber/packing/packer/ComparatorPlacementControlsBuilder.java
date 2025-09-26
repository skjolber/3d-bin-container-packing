package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;

public class ComparatorPlacementControlsBuilder extends AbstractPlacementControlsBuilder<Placement, ComparatorPlacementControlsBuilder> {

	protected Comparator<Placement> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public ComparatorPlacementControlsBuilder withPlacementComparator(Comparator<Placement> intermediatePlacementResultComparator) {
		this.placementComparator = intermediatePlacementResultComparator;
		return this;
	}

	public ComparatorPlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}
	
	@Override
	public ComparatorPlacementControls build() {
		return new ComparatorPlacementControls(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, placementComparator, boxItemComparator);
	}

}
