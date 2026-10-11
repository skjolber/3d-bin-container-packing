package com.github.skjolber.packing.api.validator.placement;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;

public interface PlacementValidatorBuilder {
	
	PlacementValidatorBuilder withContainer(Container container);
	
	PlacementValidatorBuilder withPlacements(List<Placement> placements);
	
	PlacementValidatorBuilder withStabilityValidator(StabilityValidator stabilityValidator);

	PlacementValidator build();
	
}
