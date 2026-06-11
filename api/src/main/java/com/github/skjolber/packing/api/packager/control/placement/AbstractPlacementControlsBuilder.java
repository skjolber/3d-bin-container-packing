package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControlsBuilder<R extends Placement> implements PlacementControlsBuilder<R> {

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
	
	public AbstractPlacementControlsBuilder<R> withOrder(Order order) {
		this.order = order;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder<R> withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder<R> withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}
	
	public AbstractPlacementControlsBuilder<R> withStack(Stack stack) {
		this.stack = stack;
		return this;
	}
	
	public AbstractPlacementControlsBuilder<R> withLoadIdenticalBox(boolean loadIdenticalBox) {
		this.loadIdenticalBox = loadIdenticalBox;
		return this;
	}
	
	@Override
	public AbstractPlacementControlsBuilder<R> withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public AbstractPlacementControlsBuilder<R> withPointCalculator(PointCalculator pointCalculator) {
		this.pointCalculator = pointCalculator;
		return this;
	}
	
	protected boolean isMaxLoad() {
		return maxLoadWeight || maxLoadPressure || maxLoadBoxCount;
	}
	
	@Override
	public AbstractPlacementControlsBuilder<R> withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		return this;
	}

}
