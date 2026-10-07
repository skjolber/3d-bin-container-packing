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

/**
 * 
 * Load aware placement controls which calculates support area.
 * 
 */

public class SupportPlacementControls extends AbstractComparatorPlacementControls {

	protected final StackSupportIndex supportIndex = new StackSupportIndex();

	// whether candidates can be compared assuming full support before calculating their support
	protected final boolean skipBySupport;

	public SupportPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			PlacementComparator placementComparator, BoxItemComparator boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		this.skipBySupport = placementComparator != null && placementComparator.prefersHigherSupportedArea();
	}

	public Placement getPlacement(int offset, int length) {
		Placement result = null;
		
		// max volume and weight should already be accounted for by packager
		
		for (int i = offset; i < offset + length; i++) {
			RemainingBoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();
			
			if(order == Order.NONE) {
				// is there any point in testing this box?
				//
				// a negative integer, zero, or a positive integer as the 
				// first argument is less than, equal to, or greater than the
			    // second.
				if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem.getBoxItem()) >= 0) {
					continue;
				}
			}
			
			PointSource points = pointControls.getPoints(boxItem);

			for (BoxStackValue stackValue : box.getStackValues()) {
				for (Point point3d : points) {
					if(stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					
					if(result != null && skipBySupport) {
						result = selectPlacementIfSupported(result, point3d, boxItem, stackValue);
						continue;
					}

					Placement placementResult = createPlacement(point3d, boxItem, stackValue);
					if(placementResult == null) {
						continue;
					}
					
					result = selectPlacement(result, placementResult);
				} 
			}
			
			if(order == Order.CHRONOLOGICAL) {
				// even if null
				break;
			}
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}			
		}
		return result;
	}

	/**
	 * Compare the candidate assuming full support first, and calculate its support only if it could
	 * be selected. Same result as calculating the support of every candidate, as the comparator never
	 * prefers less support.
	 */
	private Placement selectPlacementIfSupported(Placement result, Point point, RemainingBoxItem boxItem, BoxStackValue stackValue) {
		Placement placement = acquirePlacement();
		placement.setStackValue(boxItem, stackValue);
		placement.setPoint(point);
		placement.setSupportedArea(stackValue.getArea());
		if(placementComparator.compare(result, placement) >= 0) {
			recyclePlacement(placement);
			return result;
		}
		if(point.getMinZ() != 0 && !point.isSupportedXYPlane(stackValue)) {
			placement.setSupportedArea(supportIndex.calculateAreaSupport(stack.getPlacements(), point.getMinX(), point.getMinY(), point.getMinZ(), stackValue));
		}
		return selectPlacement(result, placement);
	}

	protected Placement createPlacement(Point point, RemainingBoxItem boxItem, BoxStackValue stackValue) {
		Placement placement = acquirePlacement();
		placement.setStackValue(boxItem, stackValue);
		placement.setPoint(point);
		if(point.getMinZ() == 0 || point.isSupportedXYPlane(stackValue)) {
			placement.setSupportedArea(stackValue.getArea());
		} else {
			placement.setSupportedArea(supportIndex.calculateAreaSupport(stack.getPlacements(), point.getMinX(), point.getMinY(), point.getMinZ(), stackValue));
		}
		return placement;
	}

	/**
	 * A box placed under boxes which are already there (into the gap under an overhang) supports them too: recalculate
	 * their supported area. Recalculating (rather than adding the contact area) gives the same result however often
	 * this is called.
	 */
	@Override
	public void accepted(Placement placement) {
		List<Placement> placements = stack.getPlacements();
		// the placement is the last in the stack
		updateSupportedAreaAbove(placement, placements, placements.size() - 1);
	}

	/**
	 * Recalculate the supported area of the placements which rested on placements which are rolled back (the last
	 * placements of the stack).
	 */
	@Override
	public void undo(List<Placement> placements) {
		List<Placement> remaining = stack.getPlacements().subList(0, stack.size() - placements.size());
		for(int i = 0; i < placements.size(); i++) {
			updateSupportedAreaAbove(placements.get(i), remaining, remaining.size());
		}
	}

	/**
	 * @param below a placement
	 * @param placements the placements of the stack
	 * @param count the number of placements before {@code below}, at the start of {@code placements}
	 */
	private void updateSupportedAreaAbove(Placement below, List<Placement> placements, int count) {
		int z = below.getAbsoluteEndZ() + 1;
		for(int i = 0; i < count; i++) {
			Placement above = placements.get(i);
			if(above.getAbsoluteZ() == z && above.intersects2D(below)) {
				above.setSupportedArea(supportIndex.calculateAreaSupport(placements, above.getAbsoluteX(), above.getAbsoluteY(), above.getAbsoluteZ(), above.getStackValue()));
			}
		}
	}
}
