package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.ep.Point3D;

@SuppressWarnings("rawtypes")
public class Point3DAssert extends AbstractPoint3DAssert<Point3DAssert, Point3D> {

	public static Point3DAssert assertThat(Point3D actual) {
		return new Point3DAssert(actual);
	}

	public Point3DAssert(Point3D actual) {
		super(actual, Point3DAssert.class);
	}

}
