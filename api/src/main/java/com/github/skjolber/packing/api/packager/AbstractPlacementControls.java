package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;

public abstract class AbstractPlacementControls<R extends Placement> implements PlacementControls<R> {

	protected BoxItemSource boxItems;
	protected int boxItemsStartIndex = -1;
	protected int boxItemsEndIndex = -1;
	
	protected PointControls pointControls;
	protected ExtremePoints extremePoints;
	protected Container container;
	protected Stack stack;
	protected BoxPriority priority;
	
	public AbstractPlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, ExtremePoints extremePoints, Container container, Stack stack,
			BoxPriority priority) {
		super();
		this.boxItems = boxItems;
		this.boxItemsStartIndex = boxItemsStartIndex;
		this.boxItemsEndIndex = boxItemsEndIndex;
		this.pointControls = pointControls;
		this.extremePoints = extremePoints;
		this.container = container;
		this.stack = stack;
		this.priority = priority;
	}
	
	
}
