package com.github.skjolber.packing.packer.laff;

import java.util.Objects;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.packer.PlacementControlsBuilderFactoryBuilder;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<B extends AbstractLargestAreaFitFirstPackagerBuilder<B>> {

	// only applies if no placementControlsBuilderFactory is provided
	protected boolean requireFullSupport;
	protected boolean calculateSupport;

	protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
	
	protected BoxItemGroupComparator boxItemGroupComparator;

	protected PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;

	protected ContainerPackingStrategyFactory containerPackingStrategyFactory;
	
	public B withCalculateSupport(boolean calculateSupport) {
		this.calculateSupport = calculateSupport;
		return (B)this;
	}
	
	public B withRequireFullSupport(boolean requireFullSupport) {
		this.requireFullSupport = requireFullSupport;
		return (B)this;
	}
	
	/**
	 * Set the factory which selects the container packing strategy: which containers to use, and in which order.
	 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
	 * (in preference order) which holds the boxes.
	 *
	 * @param factory container packing strategy factory
	 * @return this builder
	 */
	public B withContainerPackingStrategyFactory(ContainerPackingStrategyFactory factory) {
		this.containerPackingStrategyFactory = Objects.requireNonNull(factory);
		return (B)this;
	}

	public B withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
		this.intermediatePackagerResultComparator = comparator;
		return (B)this;
	}
	

	public B withBoxItemGroupComparator(BoxItemGroupComparator boxItemGroupComparator) {
		this.boxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}
	
	public B withPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory placementControlsBuilderFactory) {
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}

	/**
	 * Configure the placement controls with a consumer, to customize the ranking of boxes and positions without creating the
	 * controls factory yourself, like the plain packager's. The consumer's {@code withCalculateSupport(..)} and
	 * {@code withRequireFullSupport(..)} have the same effect as this builder's options of the same name, which must not be set
	 * as well. Configure {@linkplain #withFirstPlacementControlsBuilderFactory(Consumer) the first placement controls} too when the
	 * options must apply throughout the packing.
	 *
	 * @param consumer configures the placement controls
	 * @return this builder
	 */
	public B withPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder> consumer) {
		PlacementControlsBuilderFactoryBuilder b = new PlacementControlsBuilderFactoryBuilder();
		consumer.accept(b);

		this.placementControlsBuilderFactory = b.build();
		return (B)this;
	}

	/**
	 * @return the configured placement controls, or the default controls (with this builder's support options)
	 */
	protected PlacementControlsBuilderFactory getPlacementControlsBuilderFactory() {
		if(placementControlsBuilderFactory != null) {
			return placementControlsBuilderFactory;
		}
		return new PlacementControlsBuilderFactoryBuilder()
				.withCalculateSupport(calculateSupport)
				.withRequireFullSupport(requireFullSupport)
				.build();
	}

	/**
	 * @return the configured first placement controls, or the default controls (with this builder's support options)
	 */
	protected PlacementControlsBuilderFactory getFirstPlacementControlsBuilderFactory() {
		if(firstPlacementControlsBuilderFactory != null) {
			return firstPlacementControlsBuilderFactory;
		}
		return new PlacementControlsBuilderFactoryBuilder()
				.withCalculateSupport(calculateSupport)
				.withRequireFullSupport(requireFullSupport)
				.buildFirst();
	}
	
	/**
	 * The support options only apply to the default placement controls.
	 */
	protected void checkSupportOptions() {
		if((placementControlsBuilderFactory != null || firstPlacementControlsBuilderFactory != null) && (requireFullSupport || calculateSupport)) {
			throw new IllegalStateException("Support options only apply to the default placement controls: configure support with the placement controls");
		}
	}

	public B withFirstPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory placementControlsBuilderFactory) {
		this.firstPlacementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}

	/**
	 * Configure the placement controls of the first placement of a level with a consumer, see
	 * {@link #withPlacementControlsBuilderFactory(Consumer)}. By default the box with the largest area is placed first, at the
	 * lowest z.
	 *
	 * @param consumer configures the first placement controls
	 * @return this builder
	 */
	public B withFirstPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder> consumer) {
		PlacementControlsBuilderFactoryBuilder b = new PlacementControlsBuilderFactoryBuilder();
		consumer.accept(b);

		this.firstPlacementControlsBuilderFactory = b.buildFirst();
		return (B)this;
	}
	
}