package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

public abstract class AbstractComparatorPlacementControls extends AbstractPlacementControls {

	protected PlacementComparator placementComparator;
	protected Comparator<BoxItem> boxItemComparator;
	private Placement recyclablePlacement;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	/**
	 * Returns a placement which is not referenced by the stack or by the current
	 * best candidate. Placement controls are created per packing operation, so a
	 * single recycled loser is sufficient to avoid allocating for every point and
	 * rotation considered by the comparator.
	 */
	protected Placement acquirePlacement() {
		Placement placement = recyclablePlacement;
		if(placement == null) {
			return new Placement();
		}

		recyclablePlacement = null;
		placement.clearLoad();
		placement.setProperties(null);
		placement.setIndex(0);
		return placement;
	}

	/**
	 * Retain a placement which is no longer a candidate, for reuse by {@link #acquirePlacement()}.
	 */
	protected void recyclePlacement(Placement placement) {
		recyclablePlacement = placement;
	}

	/**
	 * Selects the better placement and retains the loser for the next candidate.
	 */
	protected Placement selectPlacement(Placement current, Placement candidate) {
		if(current == null) {
			return candidate;
		}
		if(placementComparator.compare(current, candidate) >= 0) {
			recyclablePlacement = candidate;
			return current;
		}

		recyclablePlacement = current;
		return candidate;
	}

	// bounds of the points in boundsSource, for skipping stack values that fit no point
	private PointSource boundsSource;
	private long boundsMaxArea;
	private int boundsMaxDx;
	private int boundsMaxDy;
	private int boundsMaxDz;

	/**
	 * Forget cached point bounds; call at the start of each placement search, since points change between searches.
	 */
	protected void resetPointBounds() {
		boundsSource = null;
	}

	/**
	 * Quick rejection: whether a stack value can fit any of the points, by comparing it to the largest
	 * area and the largest point dimensions. A false result means no point fits; a true result means
	 * the points must still be checked one by one. The bounds are cached per point source until
	 * {@link #resetPointBounds()}.
	 */
	protected boolean canFitAny(PointSource points, BoxStackValue stackValue) {
		if(points != boundsSource) {
			long maxArea = 0;
			int maxDx = 0;
			int maxDy = 0;
			int maxDz = 0;
			for (Point point : points) {
				if(point.getArea() > maxArea) {
					maxArea = point.getArea();
				}
				if(point.getDx() > maxDx) {
					maxDx = point.getDx();
				}
				if(point.getDy() > maxDy) {
					maxDy = point.getDy();
				}
				if(point.getDz() > maxDz) {
					maxDz = point.getDz();
				}
			}
			boundsMaxArea = maxArea;
			boundsMaxDx = maxDx;
			boundsMaxDy = maxDy;
			boundsMaxDz = maxDz;
			boundsSource = points;
		}
		return stackValue.getArea() <= boundsMaxArea && stackValue.getDx() <= boundsMaxDx && stackValue.getDy() <= boundsMaxDy && stackValue.getDz() <= boundsMaxDz;
	}
}
