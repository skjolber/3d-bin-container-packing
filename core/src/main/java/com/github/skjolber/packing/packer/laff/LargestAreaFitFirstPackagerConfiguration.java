package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.Point2D;

public interface LargestAreaFitFirstPackagerConfiguration<P extends Point2D> {

	StackableComparator getFirstComparator();
	
	StackValueComparator<P> getFirstStackValueComparator();

	StackableComparator getNextComparator();
	
	StackValueComparator<P> getNextStackValueComparator();
}
