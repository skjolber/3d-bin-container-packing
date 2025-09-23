package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;

public interface PlacementControlsBuilder<R extends Placement, B extends PlacementControlsBuilder<R, B>> {

	B withExtremePoints(ExtremePoints extremePoints);

	B withBoxItems(BoxItemSource boxItems, int offset, int length);

	B withPointControls(PointControls pointControls);

	B withStack(Stack stack);

	B withContainer(Container container);

	B withPriority(BoxPriority priority);

	PlacementControls<R> build();
	
}
