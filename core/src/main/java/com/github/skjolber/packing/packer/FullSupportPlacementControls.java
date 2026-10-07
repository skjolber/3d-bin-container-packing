package com.github.skjolber.packing.packer;

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

public class FullSupportPlacementControls extends AbstractComparatorPlacementControls {

	public FullSupportPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			PlacementComparator placementComparator, BoxItemComparator boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
	}

	protected final StackSupportIndex supportIndex = new StackSupportIndex();

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
					
					if(point3d.getMinZ() == 0 || point3d.isSupportedXYPlane(stackValue)) {
						Placement placementResult = createPlacement(boxItem, stackValue, point3d);
						if(placementResult == null) {
							continue;
						}
						
						result = selectPlacement(result, placementResult);
					} else {
						// a valid candidate is fully supported: compare it first, and check the
						// support only if it would be selected
						Placement placementResult = createPlacement(boxItem, stackValue, point3d);
						if(placementResult == null) {
							continue;
						}
						if(result != null && placementComparator.compare(result, placementResult) >= 0) {
							recyclePlacement(placementResult);
							continue;
						}
						if(!supportIndex.isFullSupport(stack.getPlacements(), point3d.getMinX(), point3d.getMinY(), point3d.getMinZ(), stackValue)) {
							recyclePlacement(placementResult);
							continue;
						}
						result = selectPlacement(result, placementResult);
					}
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
		
		if(result != null) {
			return result;
		}
		
		return getFullySupportedPlacement(offset, length);
	}

	protected Placement getFullySupportedPlacement(int offset, int length) {
		
		Placement result = null;
		// try placing boxes within points
		// pick the points where an underlying placement exists.
		
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
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					
					// check viable inner points in the same plane
					// use the corners of underlying placements
					int z = point3d.getMinZ() - 1;
					// the last start which keeps the box within the point
					int minX = point3d.getMaxX() - stackValue.getDx() + 1;
					int minY = point3d.getMaxY() - stackValue.getDy() + 1;

					// a placement below must at least reach the end of the box placed at the point origin
					int minMaxX = point3d.getMinX() + stackValue.getDx() - 1;
					int minMaxY = point3d.getMinY() + stackValue.getDy() - 1;

					if(z < 0) {
						// on the floor: always fully supported at the point origin
						continue;
					}
					
					for (Placement candidate : stack.getPlacements()) {
						if (candidate.getAbsoluteEndZ() != z) {
							continue;
						}
						
						if(candidate.getAbsoluteX() > minX || candidate.getAbsoluteEndX() < minMaxX) {
							continue;
						}
						
						if(candidate.getAbsoluteY() > minY || candidate.getAbsoluteEndY() < minMaxY) {
							continue;
						}
						
						int x = candidate.getAbsoluteX();
						if(x < point3d.getMinX()) {
							x = point3d.getMinX();
						}
						int y = candidate.getAbsoluteY();
						if(y < point3d.getMinY()) {
							y = point3d.getMinY();
						}
						
						if(!supportIndex.isFullSupport(stack.getPlacements(), x, y, point3d.getMinZ(), stackValue) ) {
							continue;
						}
						
						Placement placement = createPlacement(boxItem, stackValue, point3d.getIndex(), x, y, point3d.getMinZ());
						
						result = selectPlacement(result, placement);
					}
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

	protected Placement createPlacement(RemainingBoxItem boxItem, BoxStackValue stackValue, int index, int x, int y, int z) {
		Placement placement = acquirePlacement();
		placement.setStackValue(boxItem, stackValue);
		placement.setPoint(index, x, y, z);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
	
	protected Placement createPlacement(RemainingBoxItem boxItem, BoxStackValue stackValue, Point point) {
		Placement placement = acquirePlacement();
		placement.setStackValue(boxItem, stackValue);
		placement.setPoint(point);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
}
