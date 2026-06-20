package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControlsBuilder implements PlacementControlsBuilder {

	protected BoxItemSource boxItems;
	
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	
	protected boolean maxLoadWeight;
	protected boolean maxLoadPressure;
	protected boolean maxLoadBoxCount;
	
	protected boolean loadIdenticalBox;
	
	public AbstractPlacementControlsBuilder withOrder(Order order) {
		this.order = order;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}
	
	public AbstractPlacementControlsBuilder withStack(Stack stack) {
		this.stack = stack;
		return this;
	}
	
	public AbstractPlacementControlsBuilder withLoadIdenticalBox(boolean loadIdenticalBox) {
		this.loadIdenticalBox = loadIdenticalBox;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public AbstractPlacementControlsBuilder withPointCalculator(PointCalculator pointCalculator) {
		this.pointCalculator = pointCalculator;
		return this;
	}
	
	protected boolean isMaxLoad() {
		return maxLoadWeight || maxLoadPressure || maxLoadBoxCount;
	}
	
	@Override
	public AbstractPlacementControlsBuilder withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		return this;
	}

}
