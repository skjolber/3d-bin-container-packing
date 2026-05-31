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

public abstract class AbstractComparatorPlacementControls<T extends Placement> extends AbstractPlacementControls<T> {

	protected Comparator<T> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, Comparator<T> placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

}
