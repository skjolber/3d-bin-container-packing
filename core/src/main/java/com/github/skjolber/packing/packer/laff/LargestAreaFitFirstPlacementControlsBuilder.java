package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControlsBuilder;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.packer.ComparatorPlacementControls;

public class LargestAreaFitFirstPlacementControlsBuilder extends AbstractPlacementControlsBuilder<Placement> {

	protected Comparator<Placement> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public LargestAreaFitFirstPlacementControlsBuilder withIntermediatePlacementComparator(Comparator<Placement> intermediatePlacementResultComparator) {
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		return this;
	}

	public LargestAreaFitFirstPlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	@Override
	public ComparatorPlacementControls build() {
		if(boxItemComparator == null) {
			boxItemComparator = new VolumeThenWeightBoxItemComparator();
		}
		return new ComparatorPlacementControls(boxItems, boxItemsEndIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority, intermediatePlacementResultComparator, boxItemComparator);
	}

}
