package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Order;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControls<R extends Placement> implements PlacementControls<R> {

	public static boolean isFullSupport(List<Placement> placements, int minX, int minY, int minZ, BoxStackValue stackValue) {
		return stackValue.getArea() == calculateAreaSupport(placements, minX, minY, minZ, stackValue);
	}

	public static long calculateAreaSupport(List<Placement> placements, int minX, int minY, int minZ, BoxStackValue stackValue) {
		long sum = 0;

		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive
		
		long max = (maxX - minX + 1) * (maxY - minY + 1);
		
		int z = minZ - 1;
		
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
			    	break;
			    }
			}
		}
		
		return sum;
	}
	
	
	protected BoxItemSource boxItems;
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	
	public AbstractPlacementControls(BoxItemSource boxItems, PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack, Order order) {
		super();
		this.boxItems = boxItems;
		this.pointControls = pointControls;
		this.pointCalculator = pointCalculator;
		this.container = container;
		this.stack = stack;
		this.order = order;
	}

	public static boolean canStackOneMore(Placement candidate) {
		return canStackLevels(candidate, 1);
	}

	public static boolean canStackLevels(Placement candidate, int levels) {
		BoxStackValue stackValue = candidate.getStackValue();
		if(stackValue.isMaxLoadBoxCount()) {
			if(stackValue.getMaxLoadBoxCount() < levels) {
				return false;
			}
		}
		
		levels++;
		for (PlacementLoad placementLoad : candidate.getSupporters()) {
			if(!canStackLevels(placementLoad.getPlacement(), levels)) {
				return false;
			}
		}
		
		return true;
	}

	public static boolean isWithinMaxLoadBoxCount(List<Placement> supporters) {
		for (Placement candidate : supporters) {
			if(!canStackOneMore(candidate)) {
				return false;
			}
		}
		return true;
	}
}
