package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValuePointFilter;
import com.github.skjolber.packing.api.StackableFilter;

public interface LargestAreaFitFirstPackagerConfiguration<P extends Point2D<StackPlacement>> {

	/** rough filter on whether to further compare stackables */
	StackableFilter getFirstStackableFilter();
	StackValuePointFilter<P> getFirstStackValuePointFilter();
	
	/** rough filter on whether to further compare stackables */
	StackableFilter getNextStackableFilter();
	StackValuePointFilter<P> getNextStackValuePointFilter();
	
}
