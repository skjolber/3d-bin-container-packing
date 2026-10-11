package com.github.skjolber.packing.validator;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilder;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.load.CompositeLoadValidator;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;
import com.github.skjolber.packing.validator.stability.NoStabilityValidator;

/**
 * Builds a {@linkplain DefaultPlacementValidator}: validates the load constraints which the boxes of the placements declare, and, if set, a
 * stability rule. Only the load validators which are needed are created, so no support graph is built if no box declares a load constraint.
 */

public class DefaultPlacementValidatorBuilder implements PlacementValidatorBuilder {

	/** Stateless: for placements where no box declares a load constraint */
	private static final LoadValidator NO_LOAD_CONSTRAINTS = new CompositeLoadValidator(Collections.emptyList());
	
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
		if(loadValidator == null) {
			// no box declares a load constraint: the loads are validated, there is nothing to check (and no support graph to build)
			loadValidator = NO_LOAD_CONSTRAINTS;
		}
		
		return new DefaultPlacementValidator(container, loadValidator, stabilityValidator);
	}

}
