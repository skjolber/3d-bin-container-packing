package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.Point2D;

public class DefaultLargestAreaFitFirstPackagerConfiguration<P extends Point2D> implements LargestAreaFitFirstPackagerConfiguration<P> {

	private final StackValueComparator<P> nextStackValueComparator;
	private final StackableComparator nextComparator;
	private final StackValueComparator<P> firstStackValueComparator;
	private final StackableComparator firstComparator;

	public DefaultLargestAreaFitFirstPackagerConfiguration(StackableComparator firstComparator,
			StackValueComparator<P> firstStackValueComparator, StackableComparator nextComparator,
			StackValueComparator<P> nextStackValueComparator) {
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
	public StackValueComparator<P> getFirstStackValueComparator() {
		return firstStackValueComparator;
	}

	@Override
	public StackableComparator getNextComparator() {
		return nextComparator;
	}

	@Override
	public StackValueComparator<P> getNextStackValueComparator() {
		return nextStackValueComparator;
	}


}
