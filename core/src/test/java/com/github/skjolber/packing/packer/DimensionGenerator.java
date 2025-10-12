package com.github.skjolber.packing.packer;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class DimensionGenerator extends Generator<Dimension> {
	public DimensionGenerator() {
		super(Dimension.class);
	}

	@Override
	public Dimension generate(final SourceOfRandomness sourceOfRandomness, final GenerationStatus generationStatus) {
		return new Dimension(sourceOfRandomness.nextInt(1, 1000), sourceOfRandomness.nextInt(1, 1000), sourceOfRandomness.nextInt(1, 1000));
	}
}
