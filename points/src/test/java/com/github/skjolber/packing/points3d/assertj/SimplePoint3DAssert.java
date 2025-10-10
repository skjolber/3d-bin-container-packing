package com.github.skjolber.packing.points3d.assertj;

import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

@SuppressWarnings("rawtypes")
public class SimplePoint3DAssert extends AbstractSimplePoint3DAssert<SimplePoint3DAssert, SimplePoint3D> {

	public static SimplePoint3DAssert assertThat(SimplePoint3D actual) {
		return new SimplePoint3DAssert(actual);
	}

	public SimplePoint3DAssert(SimplePoint3D actual) {
		super(actual, SimplePoint3DAssert.class);
	}

}
