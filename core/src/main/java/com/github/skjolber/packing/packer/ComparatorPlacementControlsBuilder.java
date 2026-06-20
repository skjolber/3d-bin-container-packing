package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public class ComparatorPlacementControlsBuilder implements PlacementControlsBuilder {

	protected Comparator<Placement> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	protected BoxItemSource boxItems;
	
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	
	protected boolean maxLoadWeight;
	protected boolean maxLoadPressure;
	protected boolean maxLoadBoxCount;

	protected boolean fullSupport;
	protected boolean loadIdenticalBox;

	public ComparatorPlacementControlsBuilder withOrder(Order order) {
		this.order = order;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}
	
	public ComparatorPlacementControlsBuilder withStack(Stack stack) {
		this.stack = stack;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public ComparatorPlacementControlsBuilder withPointCalculator(PointCalculator pointCalculator) {
		this.pointCalculator = pointCalculator;
		return this;
	}
	
	public ComparatorPlacementControlsBuilder withPlacementComparator(Comparator<Placement> intermediatePlacementResultComparator) {
		this.placementComparator = intermediatePlacementResultComparator;
		return this;
	}

	public ComparatorPlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		return this;
	}
	
	@Override
	public PlacementControlsBuilder withStability(boolean calculateSupport, boolean fullSupport) {
		this.fullSupport = fullSupport;
		return this;
	}
	
	@Override
	public PlacementControls build() {
		if(fullSupport) {
			throw new IllegalStateException("Full support not supported");
		}
		if(maxLoadWeight || maxLoadPressure || maxLoadBoxCount || loadIdenticalBox) {
			throw new IllegalStateException("Max load not supported");
		}
		return new ComparatorPlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
	}

	@Override
	public PlacementControlsBuilder withLoadIdenticalBox(boolean loadIdenticalBox) {
		this.loadIdenticalBox = loadIdenticalBox;
		return this;
	}

}