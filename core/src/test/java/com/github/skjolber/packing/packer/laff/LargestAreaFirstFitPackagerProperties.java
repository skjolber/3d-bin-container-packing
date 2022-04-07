package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerProperties;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;

public class LargestAreaFirstFitPackagerProperties extends AbstractPackagerProperties {
	@Override
	protected AbstractPackager<?, ?> buildPackager(final Container container) {
		return LargestAreaFitFirstPackager.newBuilder().withContainers(container).build();
	}
}
