package com.github.skjolber.packing.packer;

import java.util.List;


import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemComparator;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

public abstract class AbstractLoadWeightComparatorPlacementControls extends AbstractComparatorPlacementControls {

	protected boolean fullSupport;

	/**
	 * Whether candidates can be compared before calculating support and load: true when the comparator
	 * does not read the supported area. Then only candidates which would be selected are validated.
	 */
	protected final boolean validateSelectedOnly;

	// the point (for the current box) for which supporters and supportees are populated, if any
	protected Point populatedPoint;

	/** Utility encapsulating variant load-constraint logic and shared mutable state. */
	protected final LoadPlacementUtility util;

	protected AbstractLoadWeightComparatorPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			PlacementComparator placementComparator, BoxItemComparator boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);

		this.fullSupport = fullSupport;
		this.validateSelectedOnly = placementComparator != null && !placementComparator.usesSupportedArea();
		this.util = createLoadPlacementUtility(stack);

		int count = 0;
		for (int i = 0; i < boxItems.size(); i++) count += boxItems.get(i).getCount();
		util.initialize(count);
	}

	/** Factory method — subclasses return the appropriate utility variant. */
	protected abstract LoadPlacementUtility createLoadPlacementUtility(Stack stack);


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

		for (int i = offset; i < offset + length; i++) {
			RemainingBoxItem boxItem = boxItems.get(i);
			Box box = boxItem.getBox();

			if (order == Order.NONE) {
				if (result != null && boxItemComparator != null
						&& boxItemComparator.compare(result.getBoxItem(), boxItem.getBoxItem()) >= 0) {
					continue;
				}
			}

			PointSource points = pointControls.getPoints(boxItem);
			// supportees depend on the box height
			populatedPoint = null;

			for (Point point3d : points) {
				for (BoxStackValue stackValue : box.getStackValues()) {
					if (stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					if (!point3d.fits3D(stackValue)) {
						continue;
					}

					if(validateSelectedOnly) {
						result = selectValidPlacement(result, point3d, boxItem, stackValue);
						continue;
					}

					populate(point3d, box);
					long supportedArea = util.getSupportedAreaAtPoint(point3d, stackValue, fullSupport);
					if (supportedArea == -1L) {
						continue;
					}

					Placement placement = acquirePlacement();
					placement.setStackValue(boxItem, stackValue);
					placement.setPoint(point3d);
					placement.setSupportedArea(supportedArea);
					result = selectPlacement(result, placement);
				}
			}

			if (order == Order.CHRONOLOGICAL) {
				break;
			}
			if (order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && result != null) {
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
	 * Compare the candidate with the current best first, and calculate its support and load only if
	 * it would be selected. Same result as validating every candidate, as the comparator does not
	 * read the supported area and ties keep the current best.
	 */
	protected Placement selectValidPlacement(Placement result, Point point3d, RemainingBoxItem boxItem, BoxStackValue stackValue) {
		Box box = boxItem.getBox();
		Placement placement = acquirePlacement();
		placement.setStackValue(boxItem, stackValue);
		placement.setPoint(point3d);
		if(result != null && placementComparator.compare(result, placement) >= 0) {
			recyclePlacement(placement);
			return result;
		}
		// the candidate would be selected: check that it can be inserted (box item order, groups, extraction order,
		// obstacles), as selectPlacement(..) does
		if(checkInsertable && !isInsertable(placement)) {
			recyclePlacement(placement);
			return result;
		}
		populate(point3d, box);
		long supportedArea = util.getSupportedAreaAtPoint(point3d, stackValue, fullSupport);
		if (supportedArea == -1L) {
			recyclePlacement(placement);
			return result;
		}
		placement.setSupportedArea(supportedArea);
		if(result != null) {
			recyclePlacement(result);
		}
		return placement;
	}

	/**
	 * Populate the supporters and supportees of a point, once per point and box, and only for points
	 * where a candidate is validated.
	 */
	protected void populate(Point point3d, Box box) {
		if(populatedPoint != point3d) {
			util.populatePointSupporters(point3d);
			util.populatePointSupportees(point3d, box.getMinimumDz(), box.getMaximumDz());
			populatedPoint = point3d;
		}
	}

	/**
	 * Full-support fallback: tries all inner candidate positions (corners of
	 * underlying placements) where the box would be fully supported.
	 */
	protected Placement getFullySupportedPlacement(int offset, int length) {
		Placement result = null;

		for (int i = offset; i < offset + length; i++) {
			RemainingBoxItem boxItem = boxItems.get(i);
			Box box = boxItem.getBox();

			if (order == Order.NONE) {
				if (result != null && boxItemComparator != null
						&& boxItemComparator.compare(result.getBoxItem(), boxItem.getBoxItem()) >= 0) {
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
					placement.setBoxItem(boxItem);
					result = selectPlacement(result, placement);
				}
			}

			if (order == Order.CHRONOLOGICAL) {
				break;
			}
			if (order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && result != null) {
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
		util.accepted(placement);
	}

	@Override
	public void undo(List<Placement> placements) {
		// a group which did not fit: remove the loads of its placements
		util.undo(placements);
	}
}
