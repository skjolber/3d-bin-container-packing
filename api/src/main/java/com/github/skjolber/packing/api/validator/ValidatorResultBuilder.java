package com.github.skjolber.packing.api.validator;

import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

public interface ValidatorResultBuilder {

	public static interface PlacementValidatorBuilderFactoryBuilder {

		PlacementValidatorBuilderFactoryBuilder withStabilityValidator(StabilityValidator stabilityValidator);
	}
	
	
	public static interface ValidatorContainerItemBuilder {

		ValidatorContainerItemBuilder withManifestValidatorBuilderFactory(ManifestValidatorBuilderFactory manifestValidatorBuilderFactory);

		/**
		 * Take over the placement validation of this container item, for example to add a stability rule. Without a factory, the load constraints
		 * of the boxes are validated, and no stability rule, see {@linkplain ValidatorResultBuilder#withDefaultPlacementValidation(boolean)}.
		 * 
		 * @param placementValidatorBuilderFactory the factory, which decides what is validated
		 * @return this
		 */

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
	 * Whether the load constraints which the boxes declare (maximum load weight, pressure and box count, identical boxes only) are validated
	 * for the container items which have no placement validator builder factory. Enabled by default; a stability rule is only validated
	 * with a factory which has a stability validator.
	 * <p>
	 * A container item which has a placement validator builder factory is not affected: the factory decides what is validated. Whether the
	 * load and stability were validated is reported by {@linkplain ValidatorResult#isLoadValidated()} and
	 * {@linkplain ValidatorResult#isStabilityValidated()}.
	 * 
	 * @param defaultPlacementValidation false to skip load validation for container items without a factory (the placements are then not
	 *        validated for loads at all, so a result which breaks a load constraint is accepted)
	 * @return this
	 */

	ValidatorResultBuilder withDefaultPlacementValidation(boolean defaultPlacementValidation);

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	ValidatorResult build();

}
