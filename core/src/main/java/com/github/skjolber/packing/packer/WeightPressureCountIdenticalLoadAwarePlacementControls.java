package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;
import com.github.skjolber.packing.packer.util.WeightPressureCountIdenticalLoadAwarePlacementUtility;

/**
 * Load aware placement controls which validates weight, max-load pressure,
 * max-load box-count, and identical-only stacking constraints.
 *
 * <p>This class overrides {@link #getPlacement(int, int)} to add the
 * identical-only inner-candidate fallback in the main pass: when
 * {@code sv.isLoadIdenticalBoxOnly()} and the origin-point placement fails,
 * we probe inner positions supported by corners of underlying placements
 * before giving up on that stack value.
 */
public class WeightPressureCountIdenticalLoadAwarePlacementControls extends AbstractLoadWeightComparatorPlacementControls {

	public WeightPressureCountIdenticalLoadAwarePlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator,
			boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator,
				boxItemComparator, fullSupport);
	}

	@Override
	protected LoadPlacementUtility createUtil(Stack stack) {
		return new WeightPressureCountIdenticalLoadAwarePlacementUtility(stack);
	}

	/**
	 * Extends the base main loop: when a placement at the point origin fails
	 * <em>and</em> the stack value requires identical-box-only supporters, we
	 * additionally try inner candidate positions supported by the corners of
	 * existing placements in the same z-plane.
	 */
	@Override
	public Placement getPlacement(int offset, int length) {
		Placement result = null;

		for (int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			Box box = boxItem.getBox();

			if (order == Order.NONE) {
				if (result != null && boxItemComparator != null
						&& boxItemComparator.compare(result.getBoxItem(), boxItem) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
					continue;
				}
			}

			PointSource points = pointControls.getPoints(boxItem);

			for (Point point3d : points) {
				util.populatePointSupporters(point3d);
				util.populatePointSupportees(point3d, box.getMinimumDz(), box.getMaximumDz());

				for (BoxStackValue stackValue : box.getStackValues()) {
					if (stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					if (!point3d.fits3D(stackValue)) {
						continue;
					}

					Placement placement = util.getPlacementAtPoint(point3d, stackValue, fullSupport);

					if (placement == null && stackValue.isLoadIdenticalBoxOnly()) {
						// identical-only boxes may fit at inner positions supported by
						// the corners of existing placements in the same z-plane
						Placement p = util.findPlacementAtPointSupporters(point3d, stackValue, placementComparator);
						if (p != null && (result == null || placementComparator.compare(result, p) > 0)) {
							result = p;
						}
					}

					if (placement == null) {
						continue;
					}
					if (result != null && placementComparator.compare(result, placement) >= 0) {
						continue;
					}
					result = placement;
				}
			}

			if (order == Order.CRONOLOGICAL) {
				break;
			}
			if (order == Order.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}
		}

		if (result != null) {
			result.setIndex(stack.size());
			return result;
		}

		if (!fullSupport) {
			return null;
		}
		return getFullySupportedPlacement(offset, length);
	}
}
