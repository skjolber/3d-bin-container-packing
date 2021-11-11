package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.Point2D;

public interface LargestAreaFitFirstPackagerConfiguration {

	StackableComparator getFirstComparator();
	
	StackValueComparator<Point2D> getFirstStackValueComparator();

	StackableComparator getNextComparator();
	
	StackValueComparator<Point2D> getNextStackValueComparator();
}
