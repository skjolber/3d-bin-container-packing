package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.packer.util.LoadWeightPlacementUtil;
import com.github.skjolber.packing.packer.util.WeightLoadAwarePlacementUtil;

/**
 * Load aware placement controls which validates weight constraints only.
 */
public class WeightLoadAwarePlacementControls extends AbstractLoadWeightComparatorPlacementControls {

	public WeightLoadAwarePlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator,
			boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator,
				boxItemComparator, fullSupport);
	}

	@Override
	protected LoadWeightPlacementUtil createUtil(Stack stack) {
		return new WeightLoadAwarePlacementUtil(stack);
	}
}
