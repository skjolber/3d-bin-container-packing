package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilder;
import com.github.skjolber.packing.api.point.PointCalculator;
public interface PlacementControlsBuilder {

	PlacementControlsBuilder withPointCalculator(PointCalculator pointCalculator);

	PlacementControlsBuilder withBoxItems(BoxItemSource boxItems);

	PlacementControlsBuilder withPointControls(PointControls pointControls);

	PlacementControlsBuilder withStack(Stack stack);

	PlacementControlsBuilder withContainer(Container container);

	PlacementControlsBuilder withOrder(Order order);

	PlacementControlsBuilder withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount);

	PlacementControlsBuilder withStability(boolean calculateSupport, boolean fullSupport);	

	PlacementControlsBuilder withLoadIdenticalBox(boolean loadIdenticalBox);

	PlacementControls build();
	
}
