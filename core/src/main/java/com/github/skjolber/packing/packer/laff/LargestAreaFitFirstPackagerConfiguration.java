package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.StackValuePointFilter;

public interface LargestAreaFitFirstPackagerConfiguration<P extends Point2D<StackPlacement>> {

	/** rough filter on whether to further compare stackables */
	StackableFilter getFirstStackableFilter();

	StackValuePointFilter<P> getFirstStackValuePointFilter();

	/** rough filter on whether to further compare stackables */
	StackableFilter getNextStackableFilter();

	StackValuePointFilter<P> getNextStackValuePointFilter();

}
