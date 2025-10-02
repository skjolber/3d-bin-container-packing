package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;

public class PlainPlacementControlsBuilder extends AbstractPlacementControlsBuilder<PlainPlacement> {

	protected final Comparator<PlainPlacement> plainPlacementComparator;
	protected final Comparator<BoxItem> boxItemComparator;

	public PlainPlacementControlsBuilder(Comparator<PlainPlacement> plainPlacementComparator, Comparator<BoxItem> boxItemComparator) {
		this.plainPlacementComparator = plainPlacementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	@Override
	public PlainPlacementControls build() {
		return new PlainPlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, plainPlacementComparator, boxItemComparator);
	}

}
