package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderSupplier;

public abstract class AbstractIntermediatePlacementResultBuilder<R extends IntermediatePlacementResult, B extends AbstractIntermediatePlacementResultBuilder<R, B>> implements IntermediatePlacementResultBuilder<R, B> {

	protected Container container;
	protected ExtremePoints extremePoints;
	protected FilteredBoxItems boxItems;
	protected FilteredPointsBuilderSupplier filteredPointsBuilderSupplier;
	protected Stack stack;

	@Override
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}

	@Override
	public B withExtremePoints(ExtremePoints extremePoints) {
		this.extremePoints = extremePoints;
		return (B)this;
	}

	public B withFilteredBoxItems(FilteredBoxItems boxItems) {
		this.boxItems = boxItems;
		return (B)this;
	}

	public B withFilteredPointsBuilderSupplier(FilteredPointsBuilderSupplier filteredPointsBuilderSupplier) {
		this.filteredPointsBuilderSupplier = filteredPointsBuilderSupplier;
		return (B)this;
	}

	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}

}
