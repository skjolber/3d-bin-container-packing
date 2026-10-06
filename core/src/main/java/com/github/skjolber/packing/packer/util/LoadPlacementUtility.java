package com.github.skjolber.packing.packer.util;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.point.Point;

/**
 * Public API of the load-weight placement utility used by
 * {@code AbstractLoadWeightComparatorPlacementControls} and its subclasses.
 *
 * <p>Implementations encapsulate the variant load-constraint logic
 * (weight-only, weight+pressure+count, weight+pressure+count+identical) and
 * expose only the methods needed by the outer placement loop. Contact areas are calculated with
 * {@link Placement#overlapArea2D(int, int, int, int)}.
 */
public interface LoadPlacementUtility {

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
	 * Validates a placement at the point origin without allocating a
	 * {@link Placement}.
	 *
	 * @return supported area, or {@code -1} when a load constraint fails
	 */
	long getSupportedAreaAtPoint(Point point, BoxStackValue sv, boolean fullSupport);

	/** Connects a validated placement to the cached set of direct supporters. */
	void addSupportersLoad(Placement placement);

	/**
	 * Connects an accepted placement to its direct supporters. Unlike
	 * {@link #addSupportersLoad(Placement)}, this method must not assume that the
	 * most recently validated candidate is still cached.
	 */
	default void accepted(Placement placement) {
		addSupportersLoad(placement);
	}

	/**
	 * Undo {@link #accepted(Placement)} for placements which are removed again, for example a box item group which did
	 * not fit: their loads no longer rest on the placements below them. The placements must be the last placed, in the
	 * order they were placed. Placements which are already undone are skipped.
	 */
	default void undo(List<Placement> placements) {
		for(int i = placements.size() - 1; i >= 0; i--) {
			Placement placement = placements.get(i);
			placement.removeSupporteesAbove();
			for(PlacementLoad placementLoad : placement.getSupporters()) {
				placementLoad.getPlacement().removeLastSupportee();
			}
			placement.clearLoad();
		}
	}

	/**
	 * Scans all point-supporters for the best fully-supported placement of
	 * {@code sv} at {@code point3d}, comparing candidates via {@code comparator}.
	 *
	 * @return the best valid inner-candidate {@link Placement}, or {@code null}
	 */
	Placement findPlacementAtPointSupporters(Point point3d, BoxStackValue stackValue, PlacementComparator comparator);
}
