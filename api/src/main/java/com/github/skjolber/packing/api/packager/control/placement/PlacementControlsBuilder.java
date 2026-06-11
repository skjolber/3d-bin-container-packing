package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilder;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

public interface PlacementControlsBuilder<R extends Placement> {

	PlacementControlsBuilder<R> withPointCalculator(PointCalculator pointCalculator);

	PlacementControlsBuilder<R> withBoxItems(BoxItemSource boxItems);

	PlacementControlsBuilder<R> withPointControls(PointControls pointControls);

	PlacementControlsBuilder<R> withStack(Stack stack);

	PlacementControlsBuilder<R> withContainer(Container container);

	PlacementControlsBuilder<R> withOrder(Order order);

	PlacementControlsBuilder<R> withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount);

	PlacementControlsBuilder<R> withStability(boolean calculateSupport, boolean fullSupport);	

	PlacementControlsBuilder<R> withLoadIdenticalBox(boolean loadIdenticalBox);

	PlacementControls<R> build();
	
}
