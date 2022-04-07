package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerProperties;

public class FastBruteForcePackagerProperties extends AbstractPackagerProperties {
	@Override
	protected AbstractPackager<?, ?> buildPackager(final Container container) {
		return FastBruteForcePackager.newBuilder().withContainers(container).build();
	}
}
