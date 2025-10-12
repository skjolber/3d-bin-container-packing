package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.point.Point;

@SuppressWarnings("rawtypes")
public class Point3DAssert extends AbstractPoint3DAssert<Point3DAssert, Point> {

	public static Point3DAssert assertThat(Point actual) {
		return new Point3DAssert(actual);
	}

	public Point3DAssert(Point actual) {
		super(actual, Point3DAssert.class);
	}

}
