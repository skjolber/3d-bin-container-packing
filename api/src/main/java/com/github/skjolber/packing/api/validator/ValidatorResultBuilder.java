package com.github.skjolber.packing.api.validator;

import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder.ControlledContainerItemBuilder;
import com.github.skjolber.packing.api.PackagerResultBuilder.ObstaclesBuilder;
import com.github.skjolber.packing.api.PackagerResultBuilder.PointsBuilder;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

public interface ValidatorResultBuilder {

	public static interface PlacementValidatorBuilderFactoryBuilder {

		PlacementValidatorBuilderFactoryBuilder withStabilityValidator(StabilityValidator stabilityValidator);
	}
	
	
	public static interface ValidatorContainerItemBuilder {

		ValidatorContainerItemBuilder withManifestValidatorBuilderFactory(ManifestValidatorBuilderFactory manifestValidatorBuilderFactory);

		ValidatorContainerItemBuilder withPlacementValidatorBuilderFactory(PlacementValidatorBuilderFactory placementValidatorBuilderFactory) ;

		ValidatorContainerItemBuilder withPlacementValidatorBuilderFactory(Consumer<PlacementValidatorBuilderFactoryBuilder> consumer) ;

		ValidatorContainerItemBuilder withContainerItem(ContainerItem containerItem);
		
		ValidatorContainerItemBuilder withContainerItem(Container container, int count);
		
	}
	
	ValidatorResultBuilder withBoxItems(List<BoxItem> items);

	ValidatorResultBuilder withOrder(Order order);

	ValidatorResultBuilder withMaxContainerCount(int maxResults);

	ValidatorResultBuilder withBoxItemGroups(List<BoxItemGroup> items);

	ValidatorResultBuilder withContainerItems(List<ContainerItem> containers);

	ValidatorResultBuilder withContainerItem(Consumer<ValidatorContainerItemBuilder> consumer);

	ValidatorResultBuilder withPackagerResult(PackagerResult result);

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	ValidatorResult build();

}
