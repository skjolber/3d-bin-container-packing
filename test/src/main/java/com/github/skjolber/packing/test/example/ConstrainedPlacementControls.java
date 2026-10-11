package com.github.skjolber.packing.test.example;

import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;

/**
 * Example placement controls, which depend on the api module only. They place the first box that fits, at the first point it fits,
 * and reject a candidate which breaks any of these rules:
 * <ul>
 * <li>full support, a setting for the whole packager: a box must rest on the floor or on other boxes with its whole bottom face
 * <li>{@linkplain PlacementConstraint#GROUND_ONLY}, marked on a box: it must be on the floor
 * <li>{@linkplain PlacementConstraint#SAME_TYPE_STACK}, marked on a box: any box it rests on must have its box id
 * <li>forbidden regions, a setting for the whole packager: no box may take up space in them
 * </ul>
 * <p>
 * Forbidden regions are one way to keep boxes out of a part of the container. The normal way is the obstacles of the container
 * ({@code Container.withObstacles(..)}, or the obstacles of a container item builder): they are known to the point calculator, so the
 * space around them is used, and the built-in controls check that boxes can still be inserted. A region only vetoes candidates,
 * which is simple but has a cost: points are created next to placed boxes, so space behind a region can only be reached through
 * boxes which are next to it.
 * <p>
 * Candidates are not ranked (it is the first valid one), so list the box items in the order you want them placed. Only the corner of a
 * point is tried, and not the positions inside the free space of the point, where a box might rest on something else: a box which
 * is not allowed to rest on what is below the corner of the point is not placed there, although it would be allowed further in.
 * <p>
 * For ranking, see {@code PlacementComparator}. Where the dependency on the core module is acceptable, extend its
 * {@code ComparatorPlacementControls} instead: rejecting a candidate in its {@code createPlacement} hook (return null) is all the
 * rules above need, and it ranks the candidates and keeps the checks which the builder of this example has to reject.
 */
public class ConstrainedPlacementControls implements PlacementControls {

	private final BoxItemSource boxItems;
	private final PointControls pointControls;
	private final Stack stack;
	private final boolean requireFullSupport;
	private final List<ForbiddenRegion> forbiddenRegions;

	public ConstrainedPlacementControls(BoxItemSource boxItems, PointControls pointControls, Stack stack, boolean requireFullSupport, List<ForbiddenRegion> forbiddenRegions) {
		this.boxItems = boxItems;
		this.pointControls = pointControls;
		this.stack = stack;
		this.requireFullSupport = requireFullSupport;
		this.forbiddenRegions = forbiddenRegions;
	}

	@Override
	public Placement getPlacement(int offset, int length) {
		// the box item order is not a placement order here (see the builder), so any box item in the range can be next
		for(int i = offset; i < offset + length; i++) {
			BoxItem boxItem = boxItems.get(i);
			PlacementConstraint constraint = PlacementConstraint.of(boxItem);

			// ask the point controls for the points, not the point calculator: only then are their filters honored
			for(BoxStackValue stackValue : boxItem.getBox().getStackValues()) {
				for(Point point : pointControls.getPoints(boxItem)) {
					if(!point.fits3D(stackValue)) {
						continue;
					}
					if(!isAllowed(boxItem, constraint, stackValue, point)) {
						continue;
					}
					// the placement is not added to the stack here: the packager does that, and calls accepted(..)
					return new Placement(boxItem, stackValue, point, false);
				}
			}
		}
		// no box fits anywhere: the container is full
		return null;
	}

	private boolean isAllowed(BoxItem boxItem, PlacementConstraint constraint, BoxStackValue stackValue, Point point) {
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();

		for(int i = 0; i < forbiddenRegions.size(); i++) {
			if(forbiddenRegions.get(i).intersects(x, y, z, stackValue)) {
				return false;
			}
		}

		if(z == 0) {
			// on the floor: always fully supported, and the ground is always allowed
			return true;
		}
		if(constraint == PlacementConstraint.GROUND_ONLY) {
			return false;
		}
		// a point is above a box, but the box can be wider than what it rests on
		if(requireFullSupport && !AbstractPlacementControls.isFullSupport(stack.getPlacements(), x, y, z, stackValue)) {
			return false;
		}
		if(constraint == PlacementConstraint.SAME_TYPE_STACK && !restsOnlyOnSameType(boxItem, stackValue, x, y, z)) {
			return false;
		}
		return true;
	}

	private boolean restsOnlyOnSameType(BoxItem boxItem, BoxStackValue stackValue, int x, int y, int z) {
		List<Placement> placements = stack.getPlacements();
		for(int i = 0; i < placements.size(); i++) {
			Placement below = placements.get(i);
			// directly below: it ends right under the box, and shares some of its bottom face
			if(below.getAbsoluteEndZ() == z - 1 && below.overlapArea2D(x, x + stackValue.getDx() - 1, y, y + stackValue.getDy() - 1) > 0) {
				if(!Objects.equals(below.getBox().getId(), boxItem.getBox().getId())) {
					return false;
				}
			}
		}
		return true;
	}
}
