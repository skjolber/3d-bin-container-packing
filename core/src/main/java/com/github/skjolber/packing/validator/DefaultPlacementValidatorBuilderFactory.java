package com.github.skjolber.packing.validator;

import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilder;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

public class DefaultPlacementValidatorBuilderFactory implements PlacementValidatorBuilderFactory {

	protected StabilityValidator stabilityValidator;
	
	public DefaultPlacementValidatorBuilderFactory(StabilityValidator stabilityValidator) {
		this.stabilityValidator = stabilityValidator;
	}
	
	@Override
	public PlacementValidatorBuilder createPlacementValidatorBuilder() {
		return new DefaultPlacementValidatorBuilder().withStabilityValidator(stabilityValidator);
	}

}
