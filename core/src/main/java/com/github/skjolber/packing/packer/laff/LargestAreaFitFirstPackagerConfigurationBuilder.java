package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValuePointFilter;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.points2d.ExtremePoints;
import com.github.skjolber.packing.points2d.Point2D;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilder<P extends Point2D, B extends LargestAreaFitFirstPackagerConfigurationBuilder<P, B>> {

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
		return (best, candidate) -> {
			// return true if the candidate might be better than the current best
			return candidate.getMaximumArea() >= best.getMinimumArea(); 
		};
	}
	
 	protected StackValuePointFilter<P> createFirstStackValuePointFilter() {
 		return (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
			return stackValue1.getArea() < stackValue2.getArea(); // larger is better
		};
 	}
 	
	protected StackableFilter createNextStackableFilter() {
		return (best, candidate) -> {
			return candidate.getVolume() >= best.getVolume();
		};
	}
	
 	protected StackValuePointFilter<P> createNextStackValuePointFilter() {
 		return (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
 			if(stackable2.getVolume() == stackable1.getVolume()) {
 				return stackValue1.getArea() > stackValue2.getArea(); // smaller is better
 			}
			return stackable2.getVolume() > stackable1.getVolume(); // more is better 
		};
 	}

}
