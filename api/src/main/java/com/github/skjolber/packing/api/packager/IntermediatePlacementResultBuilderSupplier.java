package com.github.skjolber.packing.api.packager;

@FunctionalInterface
public interface IntermediatePlacementResultBuilderSupplier<R extends IntermediatePlacementResult, B extends IntermediatePlacementResultBuilder<R, B>> {

	IntermediatePlacementResultBuilder<R, B> getIntermediatePlacementResultBuilder();
}
