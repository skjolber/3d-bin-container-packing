package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public class ComparatorPlacementControlsBuilder implements PlacementControlsBuilder<Placement> {

	protected Comparator<Placement> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	protected BoxItemSource boxItems;
	protected int boxItemsStartIndex = -1;
	protected int boxItemsEndIndex = -1; // exclusive
	
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected BoxPriority priority;
	
	public ComparatorPlacementControlsBuilder withPriority(BoxPriority priority) {
		this.priority = priority;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}
	
	@Override
	public ComparatorPlacementControlsBuilder withBoxItems(BoxItemSource boxItems, int offset, int length) {
		this.boxItems = boxItems;
		this.boxItemsStartIndex = offset;
		this.boxItemsEndIndex = offset + length;
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
	public PlacementControls<Placement> build() {
		return new ComparatorPlacementControls(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, pointCalculator, container, stack, priority, placementComparator, boxItemComparator);
	}

}
