package com.github.skjolber.packing.packer.laff;

import java.util.Objects;

import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<B extends AbstractLargestAreaFitFirstPackagerBuilder<B>> {

	// only applies if no placementControlsBuilderFactory is provided
	protected boolean requireFullSupport;
	protected boolean calculateSupport;

	protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
	
	protected BoxItemGroupComparator boxItemGroupComparator;

	protected PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;

	protected ContainerStrategyFactory containerStrategyFactory;
	
	public B withCalculateSupport(boolean calculateSupport) {
		this.calculateSupport = calculateSupport;
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
	
}