package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.ExtremePoints;

public interface PlacementControlsBuilder<R extends Placement, B extends PlacementControlsBuilder<R, B>> {

	B withExtremePoints(ExtremePoints extremePoints);

	B withBoxItems(BoxItemSource boxItems, int offset, int length);

	B withPointControls(PointControls pointControls);

	B withStack(Stack stack);

	B withContainer(Container container);

	B withPriority(BoxPriority priority);

	PlacementControls<R> build();
	
}
