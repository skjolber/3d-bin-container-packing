package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

public class ComparatorIntermediatePlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder>  {

	@Override
	public ComparatorIntermediatePlacementControlsBuilder createIntermediatePlacementResultBuilder() {
		return new ComparatorIntermediatePlacementControlsBuilder();
	}

}
