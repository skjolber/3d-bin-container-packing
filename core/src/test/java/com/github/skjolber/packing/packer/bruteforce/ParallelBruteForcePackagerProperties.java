package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerProperties;
import com.github.skjolber.packing.packer.DimensionGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.InRange;
import org.junit.runner.JUnitCore;

public class ParallelBruteForcePackagerProperties extends AbstractPackagerProperties {
	@Override
	protected AbstractPackager<?, ?> buildPackager(final Container container) {
		return ParallelBruteForcePackager.newBuilder().withContainers(container).build();
	}

	public void eightBoxes2x2x2(@From(DimensionGenerator.class) Dimension boxSize,
															@InRange(min = "0", max = "9") int xVariation,
															@InRange(min = "0", max = "9") int yVariation,
															@InRange(min = "0", max = "9") int zVariation
	)
}
