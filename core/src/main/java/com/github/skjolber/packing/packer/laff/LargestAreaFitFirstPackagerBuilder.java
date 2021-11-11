package com.github.skjolber.packing.packer.laff;

import java.util.List;

import com.github.skjolber.packing.api.Container;

public class LargestAreaFitFirstPackagerBuilder {

	private List<Container> containers;

	private int checkpointsPerDeadlineCheck = 1;

	private LargestAreaFitFirstPackagerConfigurationBuilderFactory<?> configurationBuilderFactory;

	public LargestAreaFitFirstPackagerBuilder setConfigurationBuilderFactory(LargestAreaFitFirstPackagerConfigurationBuilderFactory<?> configurationBuilder) {
		this.configurationBuilderFactory = configurationBuilder;
		return this;
	}
	
	public LargestAreaFitFirstPackagerBuilder withContainers(List<Container> containers) {
		this.containers = containers;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}
	
	public LargestAreaFitFirstPackager build() {
		if(containers == null) {
			throw new IllegalStateException("Expected containers");
		}
		if(configurationBuilderFactory == null) {
			configurationBuilderFactory = new DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory();
		}
		return new LargestAreaFitFirstPackager(containers, checkpointsPerDeadlineCheck, configurationBuilderFactory);
	}
	
}
