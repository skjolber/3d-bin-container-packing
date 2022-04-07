package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerProperties;

public class PlainPackagerProperties extends AbstractPackagerProperties {
	@Override
	protected AbstractPackager<?, ?> buildPackager(final Container container) {
		return PlainPackager.newBuilder().withContainers(container).build();
	}

	@Override
	public void eightBoxes2x2x2(final Dimension boxSize, final int xVariation, final int yVariation, final int zVariation) {
		//skip this test
	}
}
