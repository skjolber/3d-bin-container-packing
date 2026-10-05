package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Unloading;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<B extends AbstractLargestAreaFitFirstPackagerBuilder<B>> {

	// only applies if no placementControlsBuilderFactory is provided
	protected boolean requireFullSupport;
	protected boolean calculateSupport;
	protected Unloading unloading = Unloading.ANY_ORDER;

	protected Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	protected PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;

	protected ContainerStrategyFactory containerStrategyFactory;
	
	public B withCalculateSupport(boolean calculateSupport) {
		this.calculateSupport = calculateSupport;
		return (B)this;
	}
	
	/**
	 * Set how the boxes are unloaded: this decides whether a box placed under boxes which are already there (for
	 * example into a gap under an overhang) may relieve the boxes below them, when boxes have load limits.
	 * Default {@link Unloading#ANY_ORDER}.
	 *
	 * @param unloading how the boxes are unloaded
	 * @return this builder
	 */
	@SuppressWarnings("unchecked")
	public B withUnloading(Unloading unloading) {
		this.unloading = java.util.Objects.requireNonNull(unloading);
		return (B)this;
	}

	public B withRequireFullSupport(boolean requireFullSupport) {
		this.requireFullSupport = requireFullSupport;
		return (B)this;
	}
	
	/**
	 * Set the factory which selects the container strategy: which containers to use, and in which order.
	 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
	 * (in preference order) which holds the boxes.
	 *
	 * @param factory container strategy factory
	 * @return this builder
	 */
	public B withContainerStrategyFactory(ContainerStrategyFactory factory) {
		this.containerStrategyFactory = Objects.requireNonNull(factory);
		return (B)this;
	}

	public B withIntermediatePackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
		this.intermediatePackagerResultComparator = comparator;
		return (B)this;
	}
	

	public B withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.boxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}
	
	public B withPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory placementControlsBuilderFactory) {
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
	public B withFirstPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory placementControlsBuilderFactory) {
		this.firstPlacementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
}