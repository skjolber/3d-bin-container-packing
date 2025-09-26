package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

public class ComparatorPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<Placement, ComparatorPlacementControlsBuilder>  {

	@Override
	public ComparatorPlacementControlsBuilder createPlacementControlsBuilder() {
		return new ComparatorPlacementControlsBuilder();
	}

}
