package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

/**
 * Utility base class encapsulating the variant load-constraint logic for
 * {@code AbstractLoadWeightComparatorPlacementControls} subclasses.
 *
 * <p>Holds the mutable support-graph state (supporters, supportees, relief-weight
 * scratch arrays) and provides shared methods (overlap area, load validation,
 * supporter-load calculation, point population, and inner placement-candidate
 * scanning).  The variant behaviour — computing the effective supportee weight/
 * pressure/count/identical constraints — is delegated to two abstract methods:
 * <ul>
 *   <li>{@link #calculateSupporteeLoad} — returns the total weight to be placed,
 *       or {@code -1} if any constraint on the supportees would be violated.</li>
 *   <li>{@link #populateSupporters} — fills {@link #placementSupporters} for a
 *       candidate bounding box and returns {@code false} if any supporter
 *       constraint (box-count, identical-only) is violated.</li>
 * </ul>
 *
 * <p>Three ready-to-use subclasses are provided, matching the three concrete
 * placement-controls classes:
 * <ul>
 *   <li>{@link WeightLoadAwarePlacementUtil} — weight only.</li>
 *   <li>{@link WeightPressureCountLoadAwarePlacementUtility} — weight, pressure, box-count.</li>
 *   <li>{@link WeightPressureCountIdenticalLoadAwarePlacementUtility} — weight, pressure,
 *       box-count, and identical-only restriction.</li>
 * </ul>
 */
public abstract class AbstractLoadWeightPlacementUtility implements LoadPlacementUtility {

	protected final Stack stack;

	protected PlacementList pointSupportees = new PlacementList();
	protected PlacementList pointSupporters = new PlacementList();
	protected PlacementList placementSupporters = new PlacementList();

	protected long[] placementAreas;
	protected long[] reliefWeights;

	protected AbstractLoadWeightPlacementUtility(Stack stack) {
		this.stack = stack;
	}

	public void initialize(int count) {
		placementAreas = new long[count];
		reliefWeights = new long[count];
		pointSupportees.ensureAdditionalCapacity(count);
		pointSupporters.ensureAdditionalCapacity(count);
		placementSupporters.ensureAdditionalCapacity(count);
	}

	// =========================================================================
	// Point population helpers
	// =========================================================================

	public void populatePointSupporters(Point point) {
		pointSupporters.clear();
		int z = point.getMinZ() - 1;
		for (Placement candidate : stack.getPlacements()) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			if (!point.intersectsXY(candidate)) {
				continue;
			}
			pointSupporters.add(candidate);
		}
	}

	public void populatePointSupportees(Point point, int minDz, int maxDz) {
		pointSupportees.clear();
		int limitMinDz = point.getMinZ() + minDz;
		int limitMaxDz = point.getMinZ() + maxDz;
		for (Placement candidate : stack.getPlacements()) {
			if (candidate.getAbsoluteZ() < limitMinDz) {
				continue;
			}
			if (candidate.getAbsoluteZ() > limitMaxDz) {
				continue;
			}
			if (!point.intersectsXY(candidate)) {
				continue;
			}
			pointSupportees.add(candidate);
		}
	}

	// =========================================================================
	// Shared instance helpers
	// =========================================================================

	protected void calculateRelifWeight(Placement placement, long reliefWeight) {
		long supportedArea = placement.getSupportedArea();
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			Placement supporter = placementLoad.getPlacement();
			long r = (reliefWeight * placementLoad.getArea()) / supportedArea;
			this.reliefWeights[supporter.getIndex()] += r;
			calculateRelifWeight(supporter, r);
		}
	}

	protected boolean isWithinMaxLoadWeightAndPressure(Placement placement, long weight, long area) {
		long effectiveWeight = weight - reliefWeights[placement.getIndex()];
		BoxStackValue sv = placement.getStackValue();
		if (sv.isMaxLoadPressure()) {
			if ((double) effectiveWeight > (double) area * sv.getMaxLoadPressure()) {
				return false;
			}
		}
		if (sv.isMaxLoadWeight()) {
			long existingWeight = 0;
			for (PlacementLoad pl : placement.getSupportees()) {
				existingWeight += pl.getWeight();
			}
			if (effectiveWeight + existingWeight > sv.getMaxLoadWeight()) {
				return false;
			}
		}
		long totalArea = placement.getSupportedArea();
		if (totalArea > 0) {
			for (PlacementLoad pl : placement.getSupporters()) {
				long weightShare = (effectiveWeight * pl.getArea()) / totalArea;
				if (!isWithinMaxLoadWeightAndPressure(pl.getPlacement(), weightShare, pl.getArea())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Calculates total overlap area of all {@link #placementSupporters} and validates
	 * load constraints in a single pass using cached per-supporter areas.
	 *
	 * @return total supported area, or {@code -1} if any load constraint is violated
	 */
	public long calculateSupportAndValidateSupporterLoad(BoxStackValue stackValue,
			int absoluteX, int absoluteY, long weight) {
		int n = placementSupporters.size();
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;
		long totalOverlapArea = 0;
		for (int i = 0; i < n; i++) {
			placementAreas[i] = LoadPlacementUtility.overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placementSupporters.get(i));
			totalOverlapArea += placementAreas[i];
		}
		for (int i = 0; i < n; i++) {
			long weightShare = (weight * placementAreas[i]) / totalOverlapArea;
			if (!isWithinMaxLoadWeightAndPressure(placementSupporters.get(i), weightShare, placementAreas[i])) {
				return -1;
			}
		}
		return totalOverlapArea;
	}

	public long calculateSupporteeWeight(BoxStackValue sv, Point point) {
		int minX = point.getMinX();
		int minY = point.getMinY();
		return calculateSupporteeLoad(sv, minX, minY, point.getMinZ(), minX + sv.getDx() - 1, minY + sv.getDy() - 1);
	}

	// =========================================================================
	// Abstract variant methods
	// =========================================================================

	/**
	 * Computes the total weight that would be imposed on supporters by placing
	 * {@code sv} at the given bounding box, and validates all applicable load
	 * constraints on the supportees above.
	 *
	 * @return effective placement weight (including the box's own weight), or
	 *         {@code -1} if any constraint is violated
	 */
	public abstract long calculateSupporteeLoad(BoxStackValue sv,
			int minX, int minY, int minZ, int maxX, int maxY);

	/**
	 * Populates {@link #placementSupporters} with all supporters for the bounding
	 * box defined by {@code sv} placed at the point origin.
	 *
	 * @return {@code false} if any supporter violates box-count or identical-only
	 *         constraints; {@code true} otherwise
	 */
	public boolean populateSupporters(BoxStackValue sv, Point point) {
		int minX = point.getMinX();
		int minY = point.getMinY();
		return populateSupporters(sv, minX, minY, point.getMinZ(),
				minX + sv.getDx() - 1, minY + sv.getDy() - 1);
	}

	/**
	 * Populates {@link #placementSupporters} for the given bounding box.
	 *
	 * @return {@code false} if any supporter constraint is violated
	 */
	public abstract boolean populateSupporters(BoxStackValue sv,
			int minX, int minY, int minZ, int maxX, int maxY);

	// =========================================================================
	// Placement-attempt helpers
	// =========================================================================

	/**
	 * Attempts to place {@code sv} at the given point origin.
	 *
	 * @param fullSupport when {@code true}, rejects unless the box is fully supported
	 * @return a valid {@link Placement}, or {@code null} if any constraint fails
	 */
	public Placement getPlacementAtPoint(Point point, BoxStackValue sv, boolean fullSupport) {
		long weight = calculateSupporteeWeight(sv, point);
		if (weight == -1L) {
			return null;
		}

		long supportedArea;
		if (point.getMinZ() > 0) {
			if (!populateSupporters(sv, point)) {
				return null;
			}
			supportedArea = calculateSupportAndValidateSupporterLoad(sv, point.getMinX(), point.getMinY(), weight);
			if (supportedArea == -1L) {
				return null;
			}
			if (fullSupport && supportedArea != sv.getArea()) {
				return null;
			}
		} else {
			supportedArea = sv.getArea();
		}

		Placement placement = new Placement(sv, point);
		placement.setSupportedArea(supportedArea);
		return placement;
	}

	/**
	 * Attempts to place {@code sv} at an inner position derived from {@code candidate}
	 * (full-support fallback).  Only accepts fully-supported results.
	 *
	 * @return a valid {@link Placement}, or {@code null} if any constraint fails
	 */
	public Placement getPlacementAtCandidate(Point point3d, BoxStackValue stackValue,
			Placement candidate, int limitX, int limitY, int z, int limitMaxX, int limitMaxY) {
		if (candidate.getAbsoluteEndZ() != z) {
			return null;
		}
		if (candidate.getAbsoluteX() > limitX || candidate.getAbsoluteEndX() < limitMaxX) {
			return null;
		}
		if (candidate.getAbsoluteY() > limitY || candidate.getAbsoluteEndY() < limitMaxY) {
			return null;
		}

		int x = Math.max(candidate.getAbsoluteX(), point3d.getMinX());
		int y = Math.max(candidate.getAbsoluteY(), point3d.getMinY());
		int maxX = x + stackValue.getDx() - 1;
		int maxY = y + stackValue.getDy() - 1;

		long weight = calculateSupporteeLoad(stackValue, x, y, point3d.getMinZ(), maxX, maxY);
		if (weight == -1L) {
			return null;
		}

		if (!populateSupporters(stackValue, x, y, point3d.getMinZ(), maxX, maxY)) {
			return null;
		}

		long supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, x, y, weight);
		if (supportedArea < 0 || supportedArea != stackValue.getArea()) {
			return null;
		}

		Placement placement = new Placement(stackValue, point3d.getIndex(), x, y, point3d.getMinZ());
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}

	/**
	 * Scans all {@link #pointSupporters} for the best fully-supported placement of
	 * {@code sv} at {@code point3d}, comparing candidates via {@code comparator}.
	 *
	 * <p>This is the inner candidate-scan extracted from the full-support fallback
	 * loop in the placement controls.  The outer boxItem/point/stackValue iteration
	 * stays in the controls; this method handles everything within one
	 * (point, stackValue) pair.
	 *
	 * @return the best valid inner-candidate {@link Placement} found, or
	 *         {@code null} if no candidate produces a valid fully-supported result
	 */
	public Placement findPlacementAtPointSupporters(Point point3d, BoxStackValue stackValue, PlacementComparator comparator) {
		int z = point3d.getMinZ() - 1;
		int limitX = point3d.getMaxX() - stackValue.getDx();
		int limitY = point3d.getMaxY() - stackValue.getDy();
		int limitMaxX = point3d.getMinX() + stackValue.getDx();
		int limitMaxY = point3d.getMinY() + stackValue.getDy();

		if (z <= 0 || limitX <= 0 || limitY <= 0) {
			return null;
		}

		Placement best = null;
		for (int k = 0; k < pointSupporters.size(); k++) {
			Placement candidate = pointSupporters.get(k);
			Placement p = getPlacementAtCandidate(point3d, stackValue, candidate, limitX, limitY, z, limitMaxX, limitMaxY);
			if (p == null) {
				continue;
			}
			if (best != null && comparator.compare(best, p) >= 0) {
				continue;
			}
			best = p;
		}
		return best;
	}
}
