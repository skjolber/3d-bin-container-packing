package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.Point2D;

public class DefaultLargestAreaFitFirstPackagerConfiguration implements LargestAreaFitFirstPackagerConfiguration {

	private final StackValueComparator<Point2D> nextStackValueComparator;
	private final StackableComparator nextComparator;
	private final StackValueComparator<Point2D> firstStackValueComparator;
	private final StackableComparator firstComparator;

	public DefaultLargestAreaFitFirstPackagerConfiguration(StackableComparator firstComparator,
			StackValueComparator<Point2D> firstStackValueComparator, StackableComparator nextComparator,
			StackValueComparator<Point2D> nextStackValueComparator) {
		super();
		this.firstComparator = firstComparator;
		this.firstStackValueComparator = firstStackValueComparator;
		this.nextComparator = nextComparator;
		this.nextStackValueComparator = nextStackValueComparator;
	}

	@Override
	public StackableComparator getFirstComparator() {
		return firstComparator;
	}

	@Override
	public StackValueComparator<Point2D> getFirstStackValueComparator() {
		return firstStackValueComparator;
	}

	@Override
	public StackableComparator getNextComparator() {
		return nextComparator;
	}

	@Override
	public StackValueComparator<Point2D> getNextStackValueComparator() {
		return nextStackValueComparator;
	}


}
