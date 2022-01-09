package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point2D;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilderFactory<P extends Point2D<StackPlacement>, B extends LargestAreaFitFirstPackagerConfigurationBuilder<P, B>> {

	public abstract B newBuilder();
	
}
