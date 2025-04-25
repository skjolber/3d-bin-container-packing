package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilderFactory;

public class PlainIntermediatePlacementResultBuilderFactory implements IntermediatePlacementResultBuilderFactory<PlainIntermediatePlacementResult, PlainIntermediatePlacementResultBuilder> {

	@Override
	public PlainIntermediatePlacementResultBuilder createIntermediatePlacementResultBuilder() {
		return new PlainIntermediatePlacementResultBuilder();
	}

}
