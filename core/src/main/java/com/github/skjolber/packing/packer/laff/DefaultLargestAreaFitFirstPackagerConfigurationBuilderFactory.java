package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.StackPlacement;

public class DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory<P extends Point2D<StackPlacement>> extends LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, DefaultLargestAreaFitFirstPackagerConfigurationBuilder<P>> {

	@Override
	public DefaultLargestAreaFitFirstPackagerConfigurationBuilder<P> newBuilder() {
		return new DefaultLargestAreaFitFirstPackagerConfigurationBuilder<>();
	}

}
