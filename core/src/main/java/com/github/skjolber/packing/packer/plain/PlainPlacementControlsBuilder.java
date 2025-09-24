package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;

public class PlainPlacementControlsBuilder extends AbstractPlacementControlsBuilder<PlainPlacement, PlainPlacementControlsBuilder> {

	protected Comparator<PlainPlacement> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public PlainPlacementControlsBuilder withPlacementComparator(Comparator<PlainPlacement> intermediatePlacementResultComparator) {
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		return this;
	}

	public PlainPlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	@Override
	public PlainPlacementControls build() {
		if(boxItemComparator == null) {
			boxItemComparator = new VolumeThenWeightBoxItemComparator();
		}
		return new PlainPlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, intermediatePlacementResultComparator, boxItemComparator);
	}

}
