package com.github.skjolber.packing.api.ep;

@FunctionalInterface
public interface FilteredPointsBuilderSupplier {

	FilteredPointsBuilder<?> getFilteredPointsBuilder();
}
