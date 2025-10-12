package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControls<R extends Placement> implements PlacementControls<R> {

	protected BoxItemSource boxItems;
	protected int boxItemsStartIndex = -1;
	protected int boxItemsEndIndex = -1;
	
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	
	public AbstractPlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order) {
		super();
		this.boxItems = boxItems;
		this.boxItemsStartIndex = boxItemsStartIndex;
		this.boxItemsEndIndex = boxItemsEndIndex;
		this.pointControls = pointControls;
		this.pointCalculator = pointCalculator;
		this.container = container;
		this.stack = stack;
		this.order = order;
	}
	
	
}
