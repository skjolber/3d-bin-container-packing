package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;

public class PlainPlacementControlsBuilder extends AbstractPlacementControlsBuilder<PlainPlacement> {

	protected final Comparator<PlainPlacement> plainPlacementComparator;
	protected final Comparator<BoxItem> boxItemComparator;
	protected final boolean requireFullSupport;
	
	public PlainPlacementControlsBuilder(Comparator<PlainPlacement> plainPlacementComparator, Comparator<BoxItem> boxItemComparator, boolean requireFullSupport) {
		this.plainPlacementComparator = plainPlacementComparator;
		this.boxItemComparator = boxItemComparator;
		this.requireFullSupport = requireFullSupport;
	}

	@Override
	public PlainPlacementControls build() {
		return new PlainPlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, pointCalculator, container, stack, priority, plainPlacementComparator, boxItemComparator, requireFullSupport);
	}

}
