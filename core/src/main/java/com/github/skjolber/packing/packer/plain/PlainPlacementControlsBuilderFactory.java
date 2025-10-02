package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;

public class PlainPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<PlainPlacement> {

	protected final Comparator<PlainPlacement> placementComparator;
	protected final Comparator<BoxItem> boxItemComparator; 
	
	public PlainPlacementControlsBuilderFactory(Comparator<BoxItem> boxItemComparator, Comparator<PlainPlacement> placementComparator) {
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	public PlainPlacementControlsBuilderFactory() {
		boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
		placementComparator = new PlainPlacementComparator();
 	}
	
	@Override
	public PlainPlacementControlsBuilder createPlacementControlsBuilder() {
		return new PlainPlacementControlsBuilder(placementComparator, boxItemComparator);
	}

}
