package com.github.skjolber.packing.api.packager.control.point;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPointControlsBuilder<B extends AbstractPointControlsBuilder<B>> implements PointControlsBuilder {

	protected Stack stack;
	protected Container container;
	protected BoxItemSource items;
	protected PointSource points;
	protected BoxItemGroupSource groups;
	
	protected boolean maxLoadWeight;
	protected boolean maxLoadPressure;
	protected boolean maxLoadBoxCount;
	protected boolean maxLoadIdenticalBoxCount;

	public B withBoxItemGroups(BoxItemGroupSource groups) {
		this.groups = groups;
		return (B)this;
	}
	
	public B withPoints(PointSource points) {
		this.points = points;
		return (B)this;
	}

	public B withBoxItems(BoxItemSource input) {
		this.items = input;
		return (B)this;
	}
	
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	public B withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) {
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = maxLoadIdenticalBoxCount;
		return (B)this;
	}
	
	protected boolean isMaxLoad() {
		return maxLoadWeight || maxLoadPressure || maxLoadBoxCount || maxLoadIdenticalBoxCount;
	}
	
	public abstract PointControls build();

}
