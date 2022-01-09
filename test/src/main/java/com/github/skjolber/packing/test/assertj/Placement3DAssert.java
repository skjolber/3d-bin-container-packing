package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Placement3D;

public class Placement3DAssert extends AbstractPlacement3DAssert<Placement3DAssert, Placement3D> {

	public static Placement3DAssert assertThat(Placement3D actual) {
		return new Placement3DAssert(actual);
	}

	public Placement3DAssert(Placement3D actual) {
		super(actual, Placement3DAssert.class);
	}

}
