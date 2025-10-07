package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.packer.AbstractComparatorPlacementControls;

public class PlainPlacementControls extends AbstractComparatorPlacementControls<PlainPlacement> {

	public static long calculateAreaSupport(PointCalculator pointCalculator, Point referencePoint, BoxStackValue stackValue) {
		if(referencePoint.getMinZ() == 0) {
			return stackValue.getArea();
		}
		
		long sum = 0;

		int minX = referencePoint.getMinX();
		int minY = referencePoint.getMinY();
		
		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive
		
		long max = (maxX - minX + 1) * (maxY - minY + 1);
		
		int z = referencePoint.getMinZ() - 1;
		
		List<Placement> placements = pointCalculator.getPlacements();
		for(Placement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteEndZ() == z) {
				
				// calculate the common area
				// check too far
				if(stackPlacement.getAbsoluteX() > maxX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteY() > maxY) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndX() < minX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndY() < minY) {
					continue;
				}
				
				// placement can support the stack value
				
				// |           
				// |           |---------|
				// |           |         | 
				// |    |-----------|    |
				// |    |      |xxxx|    |
				// |    |      -----|----|
				// |    |           |
				// |    |-----------| 
				// |
				// --------------------------------
				
			    int x1 = Math.max(stackPlacement.getAbsoluteX(), minX);
			    int y1 = Math.max(stackPlacement.getAbsoluteY(), minY);
			 
			    // gives top-right point
			    // of intersection rectangle
			    int x2 = Math.min(stackPlacement.getAbsoluteEndX(), maxX);
			    int y2 = Math.min(stackPlacement.getAbsoluteEndY(), maxY);
				
			    long intersect = (x2 - x1 + 1) * (y2 - y1 + 1);
			    
			    sum += intersect;
			    
			    if(sum == max) {
			    	return stackValue.getArea();
			    }
			}
		}
		
		return sum;
	}
	
	public static long calculateXYSupportPercent(PointCalculator pointCalculator, Point referencePoint, BoxStackValue stackValue) {
		long sum = calculateAreaSupport(pointCalculator, referencePoint, stackValue);

		return (sum * 100) / stackValue.getArea();
	}

	protected final boolean requireFullSupport;
	
	public PlainPlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			BoxPriority priority, Comparator<PlainPlacement> placementComparator,
			Comparator<BoxItem> boxItemComparator, boolean requireFullSupport) {
		super(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, pointCalculator, container, stack, priority,
				placementComparator, boxItemComparator);
		
		this.requireFullSupport = requireFullSupport;
	}

	@Override
	protected PlainPlacement createPlacement(Point point, BoxStackValue stackValue) {
		long support = calculateAreaSupport(pointCalculator, point, stackValue);
		
		if(requireFullSupport && support != stackValue.getArea()) {
			return null;
		}
		
		return new PlainPlacement(stackValue, point, support);
	}

}
