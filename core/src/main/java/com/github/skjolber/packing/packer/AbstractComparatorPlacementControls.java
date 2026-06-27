package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

public abstract class AbstractComparatorPlacementControls extends AbstractPlacementControls {

	protected PlacementComparator placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

}
