package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderSupplier;

public interface IntermediatePlacementResultBuilder<R extends IntermediatePlacementResult, B extends IntermediatePlacementResultBuilder<R, B>> {

	B withContainer(Container container);
	
	B withExtremePoints(ExtremePoints extremePoints);

	B withFilteredBoxItems(FilteredBoxItems boxItems);
	
	B withFilteredPointsBuilderSupplier(FilteredPointsBuilderSupplier filteredPointsBuilderSupplier);
	
	B withStack(Stack stack);

	R build();
	
}
