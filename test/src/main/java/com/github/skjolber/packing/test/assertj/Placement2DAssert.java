package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Placement2D;

public class Placement2DAssert extends AbstractPlacement2DAssert<Placement2DAssert, Placement2D> {

	public static Placement2DAssert assertThat(Placement2D actual) {
		return new Placement2DAssert(actual);
	}

	public Placement2DAssert(Placement2D actual) {
		super(actual, Placement2DAssert.class);
	}

}
