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
			PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		this.skipBySupport = placementComparator != null && placementComparator.prefersHigherSupportedArea();
	}

	public Placement getPlacement(int offset, int length) {
		Placement result = null;
		
		// max volume and weight should already be accounted for by packager
		
		for(int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();
			
			if(order == Order.NONE) {
				// is there any point in testing this box?
				//
				// a negative integer, zero, or a positive integer as the 
				// first argument is less than, equal to, or greater than the
			    // second.
				if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem) >= 0) {
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
						result = selectPlacementIfSupported(result, point3d, stackValue);
						continue;
					}

					Placement placementResult = createPlacement(point3d, stackValue);
					if(placementResult == null) {
						continue;
					}
					
					result = selectPlacement(result, placementResult);
				} 
			}
			
			if(order == Order.CRONOLOGICAL) {
				// even if null
				break;
			}
			if(order == Order.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
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
	private Placement selectPlacementIfSupported(Placement result, Point point, BoxStackValue stackValue) {
		Placement placement = acquirePlacement();
		placement.setStackValue(stackValue);
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

	protected Placement createPlacement(Point point, BoxStackValue stackValue) {
		Placement placement = acquirePlacement();
		placement.setStackValue(stackValue);
		placement.setPoint(point);
		if(point.getMinZ() == 0 || point.isSupportedXYPlane(stackValue)) {
			placement.setSupportedArea(stackValue.getArea());
		} else {
			placement.setSupportedArea(supportIndex.calculateAreaSupport(stack.getPlacements(), point.getMinX(), point.getMinY(), point.getMinZ(), stackValue));
		}
		return placement;
	}
}
