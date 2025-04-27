package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilder;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilderFactory;

public class ComparatorIntermediatePlacementResultBuilderFactory implements IntermediatePlacementResultBuilderFactory<IntermediatePlacementResult, ComparatorIntermediatePlacementResultBuilder> {

	@Override
	public ComparatorIntermediatePlacementResultBuilder createIntermediatePlacementResultBuilder() {
		return new ComparatorIntermediatePlacementResultBuilder();
	}

}
