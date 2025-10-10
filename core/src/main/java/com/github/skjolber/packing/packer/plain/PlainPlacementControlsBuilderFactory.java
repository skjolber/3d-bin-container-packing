package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;

public class PlainPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<PlainPlacement> {

	protected final Comparator<PlainPlacement> placementComparator;
	protected final Comparator<BoxItem> boxItemComparator;
	
	protected final boolean requireFullSupport;
	
	public PlainPlacementControlsBuilderFactory(Comparator<BoxItem> boxItemComparator, Comparator<PlainPlacement> placementComparator, boolean requireFullSupport) {
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
		this.requireFullSupport = requireFullSupport;
	}

	public PlainPlacementControlsBuilderFactory() {
		this.boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
		this.placementComparator = new PlainPlacementComparator();
		this.requireFullSupport = false;
 	}
	
	@Override
	public PlainPlacementControlsBuilder createPlacementControlsBuilder() {
		return new PlainPlacementControlsBuilder(placementComparator, boxItemComparator, requireFullSupport);
	}

}
