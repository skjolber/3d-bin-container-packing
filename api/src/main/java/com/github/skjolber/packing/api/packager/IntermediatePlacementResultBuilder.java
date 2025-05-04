package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;

public interface IntermediatePlacementResultBuilder<R extends IntermediatePlacementResult, B extends IntermediatePlacementResultBuilder<R, B>> {

	B withExtremePoints(ExtremePoints extremePoints);

	B withBoxItemControls(BoxItemControls listener);

	B withStack(Stack stack);

	B withContainer(Container container);

	R build();
	
}
