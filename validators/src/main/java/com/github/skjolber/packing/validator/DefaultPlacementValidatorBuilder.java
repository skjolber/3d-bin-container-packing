package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilder;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;
import com.github.skjolber.packing.validator.stability.NoStabilityValidator;

public class DefaultPlacementValidatorBuilder implements PlacementValidatorBuilder {
	
	private Container container;
	private List<Placement> placements;
	private StabilityValidator stabilityValidator;

	public PlacementValidatorBuilder withStabilityValidator(StabilityValidator stabilityValidator) {
		this.stabilityValidator = stabilityValidator;
		return this;
	}
	
	@Override
	public PlacementValidatorBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public PlacementValidatorBuilder withPlacements(List<Placement> placements) {
		this.placements = placements;
		return this;
	}

	@Override
	public PlacementValidator build() {
		if(container == null) {
			throw new IllegalStateException("Expected container");
		}
		if(placements == null) {
			throw new IllegalStateException("Expected placements");
		}
		if(stabilityValidator == null) {
			stabilityValidator = new NoStabilityValidator();
		}
		
		// autodetect load validators based on stack value settings
		LoadValidator loadValidator = new DefaultLoadValidatorBuilder().withContainer(container).withPlacements(placements).build();
		
		return new DefaultPlacementValidator(container, loadValidator, stabilityValidator);
	}

}
