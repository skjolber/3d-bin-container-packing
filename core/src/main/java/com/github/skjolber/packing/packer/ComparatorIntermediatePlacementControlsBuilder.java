package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;

public class ComparatorIntermediatePlacementControlsBuilder extends AbstractPlacementControlsBuilder<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> {

	protected Comparator<IntermediatePlacement> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public ComparatorIntermediatePlacementControlsBuilder withIntermediatePlacementComparator(Comparator<IntermediatePlacement> intermediatePlacementResultComparator) {
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		return this;
	}

	public ComparatorIntermediatePlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}
	
	@Override
	public ComparatorIntermediatePlacementControls build() {
		return new ComparatorIntermediatePlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, intermediatePlacementResultComparator, boxItemComparator);
	}

}
