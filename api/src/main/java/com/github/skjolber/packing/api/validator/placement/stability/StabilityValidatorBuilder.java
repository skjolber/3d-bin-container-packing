package com.github.skjolber.packing.api.validator.placement.stability;

import com.github.skjolber.packing.api.Container;

public interface StabilityValidatorBuilder {
	
	StabilityValidatorBuilder withContainer(Container container);
	
	StabilityValidator build();
	
}
