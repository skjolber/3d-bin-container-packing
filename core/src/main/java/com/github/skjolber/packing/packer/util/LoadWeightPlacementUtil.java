package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

/**
 * Public API of the load-weight placement utility used by
 * {@code AbstractLoadWeightComparatorPlacementControls} and its subclasses.
 *
 * <p>Implementations encapsulate the variant load-constraint logic
 * (weight-only, weight+pressure+count, weight+pressure+count+identical) and
 * expose only the methods needed by the outer placement loop.
 *
 * <p>The static helper {@link #overlapArea} is provided here so callers do not
 * need to import the abstract implementation class.
 */
public interface LoadWeightPlacementUtil {

	/**
	 * Returns the overlap area (in units²) between the axis-aligned rectangle
	 * {@code [minX,maxX] × [minY,maxY]} and the footprint of {@code candidate}.
	 */
	static long overlapArea(int minX, int minY, int maxX, int maxY, Placement candidate) {
		int overlapMinX = Math.max(minX, candidate.getAbsoluteX());
		int overlapMinY = Math.max(minY, candidate.getAbsoluteY());
		int overlapMaxX = Math.min(maxX, candidate.getAbsoluteEndX());
		int overlapMaxY = Math.min(maxY, candidate.getAbsoluteEndY());
		return (long) (overlapMaxX - overlapMinX + 1) * (long) (overlapMaxY - overlapMinY + 1);
	}

	/** Re-initialises internal arrays to hold at least {@code count} entries. */
	void initialize(int count);

	/**
	 * Populates the internal list of placements that sit directly below
	 * {@code point} (i.e. whose top face touches {@code point.minZ - 1}).
	 * When {@code point.minZ == 0} (floor level) the list is simply cleared.
	 */
	void populatePointSupporters(Point point);

	/**
	 * Populates the internal list of placements that sit directly above
	 * {@code point} within the vertical band {@code [minZ+minDz, minZ+maxDz]}.
	 */
	void populatePointSupportees(Point point, int minDz, int maxDz);

	/**
	 * Attempts to place {@code sv} at the given point origin.
	 *
	 * @param fullSupport when {@code true}, rejects unless the box is fully supported
	 * @return a valid {@link Placement}, or {@code null} if any constraint fails
	 */
	Placement getPlacementAtPoint(Point point, BoxStackValue sv, boolean fullSupport);

	/**
	 * Scans all point-supporters for the best fully-supported placement of
	 * {@code sv} at {@code point3d}, comparing candidates via {@code comparator}.
	 *
	 * @return the best valid inner-candidate {@link Placement}, or {@code null}
	 */
	Placement findPlacementAtPointSupporters(Point point3d, BoxStackValue stackValue, PlacementComparator comparator);
}
