package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.StackPlacement;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilderFactory<P extends Point2D<StackPlacement>, B extends LargestAreaFitFirstPackagerConfigurationBuilder<P, B>> {

	public abstract B newBuilder();
	
}
