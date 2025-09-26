package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

public class PlainPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> {

	@Override
	public PlainPlacementControlsBuilder createPlacementControlsBuilder() {
		return new PlainPlacementControlsBuilder();
	}

}
