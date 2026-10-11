package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidator;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;

/**
 * 
 * Container item wrapped with some validators.
 * 
 */

public class ValidatorContainerItem extends ContainerItem {

	/** Load validation only: stateless, so shared */
	private static final PlacementValidatorBuilderFactory DEFAULT_PLACEMENT_VALIDATOR_BUILDER_FACTORY = new DefaultPlacementValidatorBuilderFactory(null);

	protected ManifestValidatorBuilderFactory manifestValidatorBuilderFactory;
	protected PlacementValidatorBuilderFactory placementValidatorBuilderFactory;

	public ValidatorContainerItem(Container container, int count) {
		super(container, count);
	}
	
	public ValidatorContainerItem(ContainerItem containerItem) {
		super(containerItem.getContainer(), containerItem.getCount());
	}

	public void setPlacementValidatorBuilderFactory(PlacementValidatorBuilderFactory placementValidatorBuilderFactory) {
		this.placementValidatorBuilderFactory = placementValidatorBuilderFactory;
	}
	
	public void setManifestValidatorBuilderFactory(ManifestValidatorBuilderFactory manifestValidatorBuilderFactory) {
		this.manifestValidatorBuilderFactory = manifestValidatorBuilderFactory;
	}

	public ManifestValidator createManifestValidator(Container container) {
		if(manifestValidatorBuilderFactory == null) {
			return new DefaultManifestValidator(container);
		}
		return manifestValidatorBuilderFactory.createManifestValidatorBuilder()
				.withContainer(container)
				.build();
	}
	
	/**
	 * @param container the container
	 * @param placements the placements to validate
	 * @return the placement validator of the factory of this container item, or, if there is none, a validator of the load constraints
	 *         which the boxes declare (and no stability rule)
	 */

	public PlacementValidator createPlacementValidator(Container container, List<Placement> placements) {
		PlacementValidatorBuilderFactory factory = placementValidatorBuilderFactory;
		if(factory == null) {
			factory = DEFAULT_PLACEMENT_VALIDATOR_BUILDER_FACTORY;
		}
		return factory.createPlacementValidatorBuilder()
				.withContainer(container)
				.withPlacements(placements)
				.build();
	}
	
	public boolean hasPlacementValidatorBuilderFactory() {
		return placementValidatorBuilderFactory != null;
	}
	
	public boolean hasManifestValidatorBuilderFactory() {
		return manifestValidatorBuilderFactory != null;
	}	
	
	public boolean hasValidator() {
		return hasManifestValidatorBuilderFactory() || hasPlacementValidatorBuilderFactory();
	}

}
