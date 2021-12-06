package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.points2d.Point2D;

public abstract class LargestAreaFitFirstPackagerConfigurationBuilderFactory<P extends Point2D, B extends LargestAreaFitFirstPackagerConfigurationBuilder<P, B>> {

	public abstract B newBuilder();
	
}