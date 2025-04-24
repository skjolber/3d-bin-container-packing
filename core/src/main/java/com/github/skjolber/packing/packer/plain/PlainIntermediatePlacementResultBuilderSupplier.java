package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilderSupplier;

public class PlainIntermediatePlacementResultBuilderSupplier implements IntermediatePlacementResultBuilderSupplier<PlainIntermediatePlacementResult, PlainIntermediatePlacementResultBuilder> {

	@Override
	public PlainIntermediatePlacementResultBuilder getIntermediatePlacementResultBuilder() {
		return new PlainIntermediatePlacementResultBuilder();
	}

}
