package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.packager.PlacementControlsBuilderFactory;

public class PlainPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> {

	@Override
	public PlainPlacementControlsBuilder createIntermediatePlacementResultBuilder() {
		return new PlainPlacementControlsBuilder();
	}

}
