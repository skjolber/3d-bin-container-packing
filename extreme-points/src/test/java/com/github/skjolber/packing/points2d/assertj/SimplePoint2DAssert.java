package com.github.skjolber.packing.points2d.assertj;

import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;

@SuppressWarnings("rawtypes")
public class SimplePoint2DAssert extends AbstractSimplePoint2DAssert<SimplePoint2DAssert, SimplePoint2D> {

	public static SimplePoint2DAssert assertThat(SimplePoint2D actual) {
		return new SimplePoint2DAssert(actual);
	}

	public SimplePoint2DAssert(SimplePoint2D actual) {
		super(actual, SimplePoint2DAssert.class);
	}

}
