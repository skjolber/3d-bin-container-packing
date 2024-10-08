package com.github.skjolber.packing.points2d.assertj;

import com.github.skjolber.packing.ep.points2d.Point2D;

@SuppressWarnings("rawtypes")
public class Point2DAssert extends AbstractPoint2DAssert<Point2DAssert, Point2D> {

	public static Point2DAssert assertThat(Point2D actual) {
		return new Point2DAssert(actual);
	}

	public Point2DAssert(Point2D actual) {
		super(actual, Point2DAssert.class);
	}

}
