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

/**
 * 
 * Load aware placement controls which calculates support area.
 * 
 */

public class SupportPlacementControls extends AbstractComparatorPlacementControls<Placement> {

	public SupportPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			Comparator<Placement> placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
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
				if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
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
					
					Placement placementResult = createPlacement(point3d, stackValue);
					if(placementResult == null) {
						continue;
					}
					
					if(result != null && placementComparator.compare(result, placementResult) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
						continue;
					}
					
					result = placementResult;
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

	protected Placement createPlacement(Point point, BoxStackValue stackValue) {
		Placement placement = new Placement(stackValue, point);
		if(point.getMinZ() == 0 || point.isSupportedXYPlane(stackValue)) {
			placement.setSupportedArea(stackValue.getArea());
		} else {
			placement.setSupportedArea(calculateAreaSupport(stack.getPlacements(), point.getMinX(), point.getMinY(), point.getMinZ(), stackValue));
		}
		return placement;
	}
}
