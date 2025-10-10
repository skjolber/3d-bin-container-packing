package com.github.skjolber.packing.api.point;

@FunctionalInterface
public interface PointSourceBuilderFactory {

	PointSourceBuilder createFilteredPointsBuilder();
	
}
