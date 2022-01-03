package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ExtremePoints;
import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValuePointFilter;
import com.github.skjolber.packing.api.StackableFilter;

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
			return stackValue2.getArea() < stackValue1.getArea(); // smaller is better
		}
		return stackable2.getVolume() > stackable1.getVolume(); // more is better 
	};
		
	public static StackValuePointFilter FIRST_STACK_VALUE_POINT_FILTER = (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
		return stackValue1.getArea() < stackValue2.getArea(); // larger is better
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
