package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControlsBuilder<R extends Placement> implements PlacementControlsBuilder<R> {

	protected BoxItemSource boxItems;
	protected int boxItemsStartIndex = -1;
	protected int boxItemsEndIndex = -1; // exclusive
	
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	
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
	public AbstractPlacementControlsBuilder<R> withBoxItems(BoxItemSource boxItems, int offset, int length) {
		this.boxItems = boxItems;
		this.boxItemsStartIndex = offset;
		this.boxItemsEndIndex = offset + length;
		return this;
	}
	
	public AbstractPlacementControlsBuilder<R> withStack(Stack stack) {
		this.stack = stack;
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

}
