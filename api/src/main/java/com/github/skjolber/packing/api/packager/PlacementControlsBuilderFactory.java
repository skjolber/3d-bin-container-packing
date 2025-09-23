package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Placement;

@FunctionalInterface
public interface PlacementControlsBuilderFactory<R extends Placement, B extends PlacementControlsBuilder<R, B>> {

	PlacementControlsBuilder<R, B> createIntermediatePlacementResultBuilder();
}
