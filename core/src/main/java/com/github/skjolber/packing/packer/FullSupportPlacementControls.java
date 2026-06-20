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

public class FullSupportPlacementControls extends AbstractComparatorPlacementControls {

	public FullSupportPlacementControls(BoxItemSource boxItems, PointControls pointControls,
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
					
					if(point3d.getMinZ() == 0 || point3d.isSupportedXYPlane(stackValue) || isFullSupport(stack.getPlacements(), point3d.getMinX(), point3d.getMinY(), point3d.getMinZ(), stackValue) ) {
						Placement placementResult = createPlacement(stackValue, point3d);
						if(placementResult == null) {
							continue;
						}
						
						if(result != null && placementComparator.compare(result, placementResult) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
							continue;
						}
						
						result = placementResult;						
					}
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
		
		if(result != null) {
			return result;
		}
		
		return getFullySupportedPlacement(offset, length);
	}

	protected Placement getFullySupportedPlacement(int offset, int length) {
		
		Placement result = null;
		// try placing boxes within points
		// pick the points where an underlying placement exists.
		
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
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					
					// check viable inner points in the same plane
					// use the corners of underlying placements
					int z = point3d.getMinZ() - 1;
					int minX = point3d.getMaxX() - stackValue.getDx();
					int minY = point3d.getMaxY() - stackValue.getDy();

					int minMaxX = point3d.getMinX() + stackValue.getDx();
					int minMaxY = point3d.getMinY() + stackValue.getDy();

					if(z <= 0 || minX <= 0 || minY <= 0) {
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
						
						if(!isFullSupport(stack.getPlacements(), x, y, point3d.getMinZ(), stackValue) ) {
							continue;
						}
						
						Placement placement = createPlacement(stackValue, point3d.getIndex(), x, y, point3d.getMinZ());
						
						if(result != null && placementComparator.compare(result, placement) >= 0) {
							continue;
						}
						
						result = placement;
					}
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

	protected Placement createPlacement(BoxStackValue stackValue, int index, int x, int y, int z) {
		Placement placement = new Placement(stackValue, index, x, y, z);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
	
	protected Placement createPlacement(BoxStackValue stackValue, Point point) {
		Placement placement = new Placement(stackValue, point);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
}
