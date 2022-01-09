package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.StackValuePointFilter;

public class DefaultLargestAreaFitFirstPackagerConfiguration<P extends Point2D<StackPlacement>> implements LargestAreaFitFirstPackagerConfiguration<P> {

	private final StackableFilter nextStackableFilter;
	private final StackValuePointFilter<P> nextStackValuePointFilter;

	private final StackableFilter firstStackableFilter;
	private final StackValuePointFilter<P> firstStackValuePointFilter;
	
	public DefaultLargestAreaFitFirstPackagerConfiguration(StackableFilter firstStackableFilter,
			StackValuePointFilter<P> firstStackValuePointFilter,
			StackableFilter nextStackableFilter, 
			StackValuePointFilter<P> nextStackValuePointFilter) {
		super();
		this.firstStackableFilter = firstStackableFilter;
		this.firstStackValuePointFilter = firstStackValuePointFilter;
		this.nextStackableFilter = nextStackableFilter;
		this.nextStackValuePointFilter = nextStackValuePointFilter;
	}

	public StackableFilter getNextStackableFilter() {
		return nextStackableFilter;
	}

	public StackValuePointFilter<P> getNextStackValuePointFilter() {
		return nextStackValuePointFilter;
	}

	public StackableFilter getFirstStackableFilter() {
		return firstStackableFilter;
	}

	public StackValuePointFilter<P> getFirstStackValuePointFilter() {
		return firstStackValuePointFilter;
	}

	


}
