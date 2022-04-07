package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerProperties;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

public class BruteForcePackagerProperties extends AbstractPackagerProperties {
	@Override
	protected AbstractPackager<?, ?> buildPackager(final Container container) {
		return BruteForcePackager.newBuilder().withContainers(container).build();
	}
}
