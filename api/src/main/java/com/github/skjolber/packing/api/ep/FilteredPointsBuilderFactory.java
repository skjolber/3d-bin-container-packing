package com.github.skjolber.packing.api.ep;

@FunctionalInterface
public interface FilteredPointsBuilderFactory {

	FilteredPointsBuilder<?> createFilteredPointsBuilder();
	
}
