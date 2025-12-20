package com.github.skjolber.packing.api.validator.placement;

import com.github.skjolber.packing.api.Container;

public interface PlacementValidatorBuilder {
	
	PlacementValidatorBuilder withContainer(Container container);
	
	PlacementValidator build();
	
}
