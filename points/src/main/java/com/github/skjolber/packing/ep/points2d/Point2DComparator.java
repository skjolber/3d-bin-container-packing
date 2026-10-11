package com.github.skjolber.packing.ep.points2d;

/** Ordering of 2D points; a custom interface rather than {@link java.util.Comparator}. */
@FunctionalInterface
public interface Point2DComparator {

	int compare(Point2D o1, Point2D o2);
}
