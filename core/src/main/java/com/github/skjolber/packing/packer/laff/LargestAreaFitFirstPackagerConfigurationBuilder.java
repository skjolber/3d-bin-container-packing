package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.ExtremePoints2D;
import com.github.skjolber.packing.points2d.Point2D;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilder<B extends LargestAreaFitFirstPackagerConfigurationBuilder<B>> {

	protected Container container;
	protected Stack stack;
	protected ExtremePoints2D<StackPlacement> extremePoints;

	public B withContainer(Container container) {
		this.container = container;
		
		return (B)this;
	}

	public B withExtremePoints(ExtremePoints2D<StackPlacement> extremePoints) {
		this.extremePoints = extremePoints;
		
		return (B)this;
	}

	public B withStack(Stack stack) {
		this.stack = stack;
		
		return (B)this;
	}

	
	public LargestAreaFitFirstPackagerConfiguration build() {
		
		StackableComparator firstComparator = createFirstComparator();
		StackValueComparator<Point2D> firstStackValueComparator = createFirstStackValueComparator();
		
		StackableComparator nextComparator = createNextComparator();
		StackValueComparator<Point2D> nextStackValueComparator = createNextStackValueComparator();
		
		return new DefaultLargestAreaFitFirstPackagerConfiguration(firstComparator, firstStackValueComparator, nextComparator, nextStackValueComparator);
		
	}

	protected StackableComparator createFirstComparator() {
		return (s1, s2) -> {
			if(s1.getMaximumArea() > s2.getMinimumArea()) {
				return 1;
			}
			return -1;
		};
	}
	
 	protected StackValueComparator<Point2D> createFirstStackValueComparator() {
 		return (point1, stackValue1, point2, stackValue2) -> {
			return Long.compare(point1.getArea(), point2.getArea());
		};
 	}

 	protected StackableComparator createNextComparator() {
 		return (s1, s2) -> {
			return Long.compare(s1.getVolume(), s2.getVolume());
		};
 	}
	
 	protected StackValueComparator<Point2D> createNextStackValueComparator() {
 		return (point1, stackValue1, point2, stackValue2) -> {
			return Long.compare(point1.getArea(), point2.getArea());
		};
 	}
}
