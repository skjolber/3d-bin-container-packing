package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.AbstractPlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControls;

public class LargestAreaFirtFirstPlacementControlsBuilder extends AbstractPlacementControlsBuilder<IntermediatePlacement, LargestAreaFirtFirstPlacementControlsBuilder> {

	protected Comparator<IntermediatePlacement> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public LargestAreaFirtFirstPlacementControlsBuilder withIntermediatePlacementComparator(Comparator<IntermediatePlacement> intermediatePlacementResultComparator) {
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		return this;
	}

	public LargestAreaFirtFirstPlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	@Override
	public ComparatorIntermediatePlacementControls build() {
		if(boxItemComparator == null) {
			boxItemComparator = new VolumeThenWeightBoxItemComparator();
		}
		return new ComparatorIntermediatePlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, intermediatePlacementResultComparator, boxItemComparator);
	}

}
