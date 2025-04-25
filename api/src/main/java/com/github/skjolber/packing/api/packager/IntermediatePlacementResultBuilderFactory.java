package com.github.skjolber.packing.api.packager;

@FunctionalInterface
public interface IntermediatePlacementResultBuilderFactory<R extends IntermediatePlacementResult, B extends IntermediatePlacementResultBuilder<R, B>> {

	IntermediatePlacementResultBuilder<R, B> createIntermediatePlacementResultBuilder();
}
