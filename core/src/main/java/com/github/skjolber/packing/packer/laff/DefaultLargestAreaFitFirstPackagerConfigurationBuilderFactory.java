package com.github.skjolber.packing.packer.laff;

public class DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory extends LargestAreaFitFirstPackagerConfigurationBuilderFactory<DefaultLargestAreaFitFirstPackagerConfigurationBuilder> {

	@Override
	public DefaultLargestAreaFitFirstPackagerConfigurationBuilder newBuilder() {
		return new DefaultLargestAreaFitFirstPackagerConfigurationBuilder();
	}

}
