package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public interface PlacementControlsBuilder<R extends Placement> {

	PlacementControlsBuilder<R> withPointCalculator(PointCalculator pointCalculator);

	PlacementControlsBuilder<R> withBoxItems(BoxItemSource boxItems, int offset, int length);

	PlacementControlsBuilder<R> withPointControls(PointControls pointControls);

	PlacementControlsBuilder<R> withStack(Stack stack);

	PlacementControlsBuilder<R> withContainer(Container container);

	PlacementControlsBuilder<R> withPriority(BoxPriority priority);

	PlacementControls<R> build();
	
}
