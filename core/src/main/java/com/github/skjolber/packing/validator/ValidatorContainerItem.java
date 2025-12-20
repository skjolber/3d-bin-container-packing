package com.github.skjolber.packing.validator;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;
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

	public ManifestValidatorBuilderFactory manifestValidatorBuilderFactory;
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
	
	public PlacementValidator createPlacementValidator(Container container) {
		if(placementValidatorBuilderFactory == null) {
			return new DefaultPlacementValidator(container);
		}
		return placementValidatorBuilderFactory.createPlacementValidatorBuilder()
				.withContainer(container)
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
