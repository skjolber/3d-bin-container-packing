package com.github.skjolber.packing.test.example;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

/**
 * Builds {@linkplain ConstrainedPlacementControls}. The packager creates a builder (with the factory) and populates it with the
 * state of the container it is about to fill, then calls {@linkplain #build()}.
 * <p>
 * Replacing the default placement controls does not retain their checks, so the configurations which the example controls do not
 * implement are rejected here (a box item order, container obstacles, box item groups and extraction orders), rather than
 * silently producing results which break the rules of the packager.
 */
public class ConstrainedPlacementControlsBuilder implements PlacementControlsBuilder {

	private final boolean requireFullSupport;
	private final List<ForbiddenRegion> forbiddenRegions;

	private BoxItemSource boxItems;
	private PointControls pointControls;
	private Stack stack;
	private Container container;
	private Order order;

	public ConstrainedPlacementControlsBuilder(boolean requireFullSupport, List<ForbiddenRegion> forbiddenRegions) {
		this.requireFullSupport = requireFullSupport;
		this.forbiddenRegions = forbiddenRegions;
	}

	@Override
	public PlacementControlsBuilder withPointCalculator(PointCalculator pointCalculator) {
		// not needed: the candidate points come from the point controls, which filter the points of the point calculator
		return this;
	}

	@Override
	public PlacementControlsBuilder withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}

	@Override
	public PlacementControlsBuilder withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}

	@Override
	public PlacementControlsBuilder withStack(Stack stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public PlacementControlsBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public PlacementControlsBuilder withOrder(Order order) {
		this.order = order;
		return this;
	}

	@Override
	public PlacementControlsBuilder withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		// the factory does not support load limits, so the packager does not accept inputs which have them
		return this;
	}

	@Override
	public PlacementControlsBuilder withStability(boolean calculateSupport, boolean areaFullSupport) {
		// the support options of the packager configure the default controls: the setting of this example is on its factory
		return this;
	}

	@Override
	public PlacementControlsBuilder withLoadIdenticalBox(boolean loadIdenticalBox) {
		return this;
	}

	@Override
	public PlacementControls build() {
		rejectUnsupported();

		return new ConstrainedPlacementControls(boxItems, pointControls, stack, requireFullSupport, forbiddenRegions);
	}

	private void rejectUnsupported() {
		// a box item order needs the check that a box is not placed under a box which was placed before it
		if(order != null && order != Order.NONE) {
			throw new IllegalStateException("Packing in a box item order is not supported: " + order);
		}
		// boxes which are already in the container take up space, and are not in the stack
		if(container != null && !container.getObstacles().isEmpty()) {
			throw new IllegalStateException("Container obstacles are not supported, use a forbidden region");
		}
		for(int i = 0; i < boxItems.size(); i++) {
			// groups are inserted one at a time, and boxes of different extraction orders must not block each other
			if(boxItems.get(i).getGroup() != null) {
				throw new IllegalStateException("Box item groups are not supported");
			}
			if(boxItems.get(i).getExtractionOrder() != boxItems.get(0).getExtractionOrder()) {
				throw new IllegalStateException("Extraction orders are not supported");
			}
		}
	}
}
