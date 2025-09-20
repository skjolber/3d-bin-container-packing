package com.github.skjolber.packing.api.ep;

@FunctionalInterface
public interface PointSourceBuilderFactory {

	PointSourceBuilder<?> createFilteredPointsBuilder();
	
}
