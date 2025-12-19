package com.github.skjolber.packing.api.validator.placement;


@FunctionalInterface
public interface PlacementValidatorBuilderFactory {
	
	PlacementValidatorBuilder createPlacementValidatorBuilder();
}