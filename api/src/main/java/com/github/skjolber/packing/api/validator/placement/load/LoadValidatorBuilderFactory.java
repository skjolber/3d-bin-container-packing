package com.github.skjolber.packing.api.validator.placement.load;


@FunctionalInterface
public interface LoadValidatorBuilderFactory {
	
	LoadValidatorBuilder createLoadValidatorBuilder();
}