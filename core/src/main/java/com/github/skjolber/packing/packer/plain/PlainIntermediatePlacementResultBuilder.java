package com.github.skjolber.packing.packer.plain;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.packer.AbstractComparatorIntermediatePlacementResultBuilder;

public class PlainIntermediatePlacementResultBuilder extends AbstractComparatorIntermediatePlacementResultBuilder<PlainIntermediatePlacementResult, PlainIntermediatePlacementResultBuilder> {

	public static long calculateXYSupportPercent(ExtremePoints extremePoints, Point referencePoint, BoxStackValue stackValue) {
		if(referencePoint.getMinZ() == 0) {
			return 100;
		}
		
		long sum = 0;

		int minX = referencePoint.getMinX();
		int minY = referencePoint.getMinY();
		
		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive
		
		long max = (maxX - minX + 1) * (maxY - minY + 1);
		
		int z = referencePoint.getMinZ() - 1;
		
		List<StackPlacement> placements = extremePoints.getPlacements();
		for(StackPlacement stackPlacement : placements) {
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
			    	break;
			    }
			}
		}
		
		return (sum * 100) / stackValue.getArea();
	}

	@Override
	protected PlainIntermediatePlacementResult createIntermediatePlacementResult(BoxItem boxItem, Point point, BoxStackValue stackValue) {
		long pointSupportPercent = calculateXYSupportPercent(extremePoints, point, stackValue);
		
		return new PlainIntermediatePlacementResult(boxItem, stackValue, point, pointSupportPercent);
	}

}
