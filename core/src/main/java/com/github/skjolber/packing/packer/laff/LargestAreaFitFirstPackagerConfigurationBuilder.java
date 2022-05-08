package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.StackValuePointFilter;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilder<P extends Point2D<StackPlacement>, B extends LargestAreaFitFirstPackagerConfigurationBuilder<P, B>> {

	public static StackableFilter FIRST_STACKABLE_FILTER = (best, candidate) -> {
		// return true if the candidate might be better than the current best
		return candidate.getMaximumArea() >= best.getMinimumArea(); 
	};

	public static StackableFilter DEFAULT_STACKABLE_FILTER = (best, candidate) -> {
		// return true if the candidate might be better than the current best
		return candidate.getVolume() >= best.getVolume();
	};
	
	public static StackValuePointFilter DEFAULT_STACK_VALUE_POINT_FILTER = (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
		if(stackable2.getVolume() == stackable1.getVolume()) {
			if(stackValue1.getArea() == stackValue2.getArea()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());
				
				return distance2 < distance1; // closest is better
			}
			return stackValue2.getArea() < stackValue1.getArea(); // smaller is better
		}
		return stackable2.getVolume() > stackable1.getVolume(); // larger volume is better 
	};
		
	public static StackValuePointFilter FIRST_STACK_VALUE_POINT_FILTER = (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
		if(stackValue1.getArea() == stackValue2.getArea()) {
			if(stackValue1.getVolume() == stackValue2.getVolume()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());
				
				return distance2 < distance1; // closest is better
			}
			return stackValue1.getVolume() < stackValue2.getVolume(); // larger volume is better 

		}
		return stackValue1.getArea() < stackValue2.getArea(); // larger area is better
	};		

	protected Container container;
	protected Stack stack;
	protected ExtremePoints<StackPlacement, P> extremePoints;

	public B withContainer(Container container) {
		this.container = container;
		
		return (B)this;
	}

	public B withExtremePoints(ExtremePoints<StackPlacement, P> extremePoints) {
		this.extremePoints = extremePoints;
		
		return (B)this;
	}

	public B withStack(Stack stack) {
		this.stack = stack;
		
		return (B)this;
	}

	public LargestAreaFitFirstPackagerConfiguration<P> build() {
		
		StackableFilter firstStackableComparator = createFirstStackableFilter();
		StackValuePointFilter<P> firstStackValuePointComparator = createFirstStackValuePointFilter();

		StackableFilter nextStackableComparator = createNextStackableFilter();
		StackValuePointFilter<P> nextStackValuePointComparator = createNextStackValuePointFilter();
		
		return new DefaultLargestAreaFitFirstPackagerConfiguration<P>(
				firstStackableComparator, firstStackValuePointComparator,
				nextStackableComparator, nextStackValuePointComparator
				);
	}

	protected StackableFilter createFirstStackableFilter() {
		return FIRST_STACKABLE_FILTER;
	}
	
 	protected StackValuePointFilter<P> createFirstStackValuePointFilter() {
 		return FIRST_STACK_VALUE_POINT_FILTER;
 	}
 	
	protected StackableFilter createNextStackableFilter() {
		return DEFAULT_STACKABLE_FILTER;
	}
	
 	protected StackValuePointFilter<P> createNextStackValuePointFilter() {
 		return DEFAULT_STACK_VALUE_POINT_FILTER;
 	}

}
