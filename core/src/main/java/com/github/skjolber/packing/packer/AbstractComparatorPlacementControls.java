package com.github.skjolber.packing.packer;

import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemComparator;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;

public abstract class AbstractComparatorPlacementControls extends AbstractPlacementControls {

	protected PlacementComparator placementComparator;
	protected BoxItemComparator boxItemComparator;
	private Placement recyclablePlacement;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, BoxItemComparator boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
		// with a box item order, the order of the placements cannot be changed afterwards (see InsertionSequencer),
		// so only boxes which can be inserted after the boxes already there are placed
		this.checkInsertion = order != null && order != Order.NONE;
		this.checkObstacles = container != null && !container.getObstacles().isEmpty();
		this.checkExtraction = hasExtractionOrders(boxItems);
		this.checkGroups = !checkInsertion && hasGroups(boxItems);
		this.checkInsertable = checkInsertion || checkObstacles || checkExtraction || checkGroups;
	}

	/**
	 * Whether a candidate which would be selected must be checked, see {@link #isInsertable(Placement)}.
	 */
	protected final boolean checkInsertable;

	/**
	 * Whether the boxes belong to box item groups, which are inserted one at a time (see
	 * {@link InsertionSequencer}): a candidate must not have to be inserted before a box of another group.
	 */
	protected final boolean checkGroups;

	private static boolean hasGroups(BoxItemSource boxItems) {
		return boxItems != null && !boxItems.isEmpty() && boxItems.get(0).getGroup() != null;
	}

	/**
	 * Whether the boxes have different extraction orders: a box must not rest on, or be in the path of, a box which is
	 * extracted earlier, and a box which is extracted later must not rest on it, or be in its path.
	 */
	protected final boolean checkExtraction;

	private static boolean hasExtractionOrders(BoxItemSource boxItems) {
		if(boxItems == null) {
			return false;
		}
		for (int i = 1; i < boxItems.size(); i++) {
			if(boxItems.get(i).getExtractionOrder() != boxItems.get(0).getExtractionOrder()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether candidates must be insertable after the boxes already in the container (obstacles), which, unlike the
	 * placed boxes, cannot be reordered afterwards.
	 */
	protected final boolean checkObstacles;

	/**
	 * Whether candidates must be insertable after the boxes already placed, see {@link ContainerAccess}.
	 */
	protected final boolean checkInsertion;

	/**
	 * @return true if the candidate can be inserted after the boxes already placed (with a box item order) and the
	 *         boxes already in the container (obstacles): none of them would rest on it, and none of them is in its path
	 *         from the container's opening
	 */
	protected boolean isInsertable(Placement candidate) {
		ContainerAccess access = container != null ? container.getAccess() : ContainerAccess.ANY;
		if(checkInsertion) {
			List<Placement> placements = stack.getPlacements();
			for (int i = 0; i < placements.size(); i++) {
				if(candidate.mustPrecede(placements.get(i), access)) {
					return false;
				}
			}
		}
		if(checkGroups) {
			Object group = InsertionSequencer.getGroupKey(candidate);
			List<Placement> placements = stack.getPlacements();
			for (int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				if(!Objects.equals(group, InsertionSequencer.getGroupKey(placement)) && candidate.mustPrecede(placement, access)) {
					return false;
				}
			}
		}
		if(checkExtraction) {
			int order = candidate.getBoxItem().getExtractionOrder();
			List<Placement> placements = stack.getPlacements();
			for (int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				int placementOrder = placement.getBoxItem().getExtractionOrder();
				if(order < placementOrder) {
					// the candidate is extracted first: the placement must be insertable before it
					if(candidate.mustPrecede(placement, access)) {
						return false;
					}
				} else if(order > placementOrder) {
					// the placement is extracted first: the candidate must be insertable before it
					if(placement.mustPrecede(candidate, access)) {
						return false;
					}
				}
			}
		}
		if(checkObstacles) {
			List<Placement> obstacles = container.getObstacles();
			for (int i = 0; i < obstacles.size(); i++) {
				if(candidate.mustPrecede(obstacles.get(i), access)) {
					return false;
				}
			}
		}
		return true;
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
		if(current != null && placementComparator.compare(current, candidate) >= 0) {
			recyclablePlacement = candidate;
			return current;
		}
		// the candidate would be selected; the current placement (if any) is insertable
		if(checkInsertable && !isInsertable(candidate)) {
			recyclablePlacement = candidate;
			return current;
		}
		if(current != null) {
			recyclablePlacement = current;
		}
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
