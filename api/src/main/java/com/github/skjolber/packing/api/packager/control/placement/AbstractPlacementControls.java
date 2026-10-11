package com.github.skjolber.packing.api.packager.control.placement;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

public abstract class AbstractPlacementControls implements PlacementControls {

	/**
	 * The floor is not counted: callers must treat {@code minZ == 0} as fully supported.
	 */
	public static boolean isFullSupport(List<Placement> placements, int minX, int minY, int minZ, BoxStackValue stackValue) {
		return stackValue.getArea() == calculateAreaSupport(placements, minX, minY, minZ, stackValue);
	}

	/**
	 * The floor is not counted (returns 0 at {@code minZ == 0}): callers must treat {@code minZ == 0} as fully supported.
	 */
	public static long calculateAreaSupport(List<Placement> placements, int minX, int minY, int minZ, BoxStackValue stackValue) {
		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive

		long max = stackValue.getArea();

		int z = minZ - 1;

		long sum = 0;
		for(Placement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteEndZ() == z) {
				sum += stackPlacement.overlapArea2D(minX, maxX, minY, maxY);
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

	public static boolean isWithinMaxLoadBoxCount(List<Placement> supporters) {
		for (Placement candidate : supporters) {
			if(!candidate.isWithinMaxLoadBoxCount(1)) {
				return false;
			}
		}
		return true;
	}
}
