package com.github.skjolber.packing.api.validator.placement.stability;


@FunctionalInterface
public interface StabilityValidatorBuilderFactory {
	
	StabilityValidatorBuilder createStabilityValidatorBuilder();
}