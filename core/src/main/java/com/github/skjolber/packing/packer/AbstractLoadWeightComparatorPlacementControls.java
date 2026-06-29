package com.github.skjolber.packing.packer;

import java.util.Comparator;
import java.util.List;

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
import com.github.skjolber.packing.packer.util.LoadWeightPlacementUtil;

public abstract class AbstractLoadWeightComparatorPlacementControls extends AbstractComparatorPlacementControls {

	protected boolean fullSupport;

	/** Utility encapsulating variant load-constraint logic and shared mutable state. */
	protected final LoadWeightPlacementUtil util;

	protected AbstractLoadWeightComparatorPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);

		this.fullSupport = fullSupport;
		this.util = createUtil(stack);

		int count = 0;
		for (int i = 0; i < boxItems.size(); i++) count += boxItems.get(i).getCount();
		util.initialize(count);
	}

	/** Factory method — subclasses return the appropriate utility variant. */
	protected abstract LoadWeightPlacementUtil createUtil(Stack stack);

	/** Re-initializes internal arrays to hold at least {@code count} entries. */
	public void initialize(int count) {
		util.initialize(count);
	}

	// =========================================================================
	// Common outer loop
	// =========================================================================

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

	/**
	 * Full-support fallback: tries all inner candidate positions (corners of
	 * underlying placements) where the box would be fully supported.
	 */
	protected Placement getFullySupportedPlacement(int offset, int length) {
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

					Placement placement = util.findPlacementAtPointSupporters(point3d, stackValue, placementComparator);
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
		}
		return result;
	}

	// =========================================================================
	// accepted() — wires load-graph after a placement is accepted
	// =========================================================================

	@Override
	public void accepted(Placement placement) {
		int z = placement.getAbsoluteZ();
		if (z == 0) return;

		int minX = placement.getAbsoluteX();
		int maxX = placement.getAbsoluteEndX();
		int minY = placement.getAbsoluteY();
		int maxY = placement.getAbsoluteEndY();
		int supportZ = z - 1;

		long totalArea = 0;
		List<Placement> stackPlacements = stack.getPlacements();
		int n = stackPlacements.size();
		for (int i = 0; i < n; i++) {
			Placement candidate = stackPlacements.get(i);
			if (candidate.getAbsoluteEndZ() != supportZ) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			totalArea += LoadWeightPlacementUtil.overlapArea(minX, minY, maxX, maxY, candidate);
		}

		if (totalArea == 0) {
			return;
		}

		placement.setSupportedArea(0);

		long weight = placement.getWeight();
		for (int i = 0; i < n; i++) {
			Placement candidate = stackPlacements.get(i);
			if (candidate.getAbsoluteEndZ() != supportZ) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			long area = LoadWeightPlacementUtil.overlapArea(minX, minY, maxX, maxY, candidate);
			long weightShare = (weight * area) / totalArea;
			candidate.addLoad(placement, area, weightShare);
		}
	}
}

