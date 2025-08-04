package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;

public abstract class AbstractIntermediatePlacementResultBuilder<R extends IntermediatePlacementResult, B extends AbstractIntermediatePlacementResultBuilder<R, B>> implements IntermediatePlacementResultBuilder<R, B> {

	protected FilteredBoxItems boxItems;
	protected int boxItemsStartIndex = -1;
	protected int boxItemsEndIndex = -1;
	
	protected PointControls pointControls;
	protected ExtremePoints extremePoints;
	protected Container container;
	protected Stack stack;
	protected BoxPriority priority;
	
	public B withPriority(BoxPriority priority) {
		this.priority = priority;
		return (B)this;
	}
	
	@Override
	public B withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return (B)this;
	}
	
	@Override
	public B withBoxItems(FilteredBoxItems boxItems, int offset, int length) {
		this.boxItems = boxItems;
		this.boxItemsStartIndex = offset;
		this.boxItemsEndIndex = offset + length;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	@Override
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}

	@Override
	public B withExtremePoints(ExtremePoints extremePoints) {
		this.extremePoints = extremePoints;
		return (B)this;
	}

}
