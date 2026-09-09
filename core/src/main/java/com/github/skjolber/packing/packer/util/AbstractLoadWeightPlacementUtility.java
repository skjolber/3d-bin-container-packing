package com.github.skjolber.packing.packer.util;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
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
 *   <li>{@link WeightLoadAwarePlacementUtility} — weight only.</li>
 *   <li>{@link WeightPressureCountLoadAwarePlacementUtility} — weight, pressure, box-count.</li>
 *   <li>{@link WeightPressureCountIdenticalLoadAwarePlacementUtility} — weight, pressure,
 *       box-count, and identical-only restriction.</li>
 * </ul>
 */
public abstract class AbstractLoadWeightPlacementUtility implements LoadPlacementUtility {

	/*
	 * Linear scans are faster for short stacks. Above this size, restricting the
	 * scan to the relevant Z levels repays the index maintenance cost.
	 */
	private static final int MIN_INDEXED_STACK_SIZE = 32;

	protected final Stack stack;

	protected PlacementList pointSupportees = new PlacementList();
	protected PlacementList pointSupporters = new PlacementList();
	protected PlacementList placementSupporters = new PlacementList();
	private Placement recyclablePlacement;

	protected long[] placementAreas;
	protected double[] reliefWeights;

	/*
	 * Stack positions sorted by bottom and top Z. The stack is mutated in LIFO
	 * order by the packagers, so insertions and removals are cheap while point
	 * queries can be restricted to the relevant Z plane or range.
	 */
	private Placement[] indexedPlacements;
	private int[] indexedMinZ;
	private int[] indexedEndZ;
	private int[] minZOrder;
	private int[] endZOrder;
	private int[] candidateIndexes;
	private int indexedSize;

	protected AbstractLoadWeightPlacementUtility(Stack stack) {
		this.stack = stack;
	}

	public void initialize(int count) {
		int capacity = count + stack.getPlacements().size();
		placementAreas = new long[capacity];
		reliefWeights = new double[capacity];
		pointSupportees.ensureAdditionalCapacity(capacity);
		pointSupporters.ensureAdditionalCapacity(capacity);
		placementSupporters.ensureAdditionalCapacity(capacity);
		if(indexedPlacements != null) {
			Arrays.fill(indexedPlacements, 0, indexedSize, null);
		}
		indexedSize = 0;
	}

	// =========================================================================
	// Point population helpers
	// =========================================================================

	public void populatePointSupporters(Point point) {
		pointSupporters.clear();
		List<Placement> placements = stack.getPlacements();
		if(placements.size() < MIN_INDEXED_STACK_SIZE) {
			int z = point.getMinZ() - 1;
			for(int i = 0; i < placements.size(); i++) {
				Placement candidate = placements.get(i);
				if(candidate.getAbsoluteEndZ() == z && point.intersectsXY(candidate)) {
					pointSupporters.add(candidate);
				}
			}
			return;
		}

		synchronizeStackIndex(placements);
		int z = point.getMinZ() - 1;
		int index = lowerBound(endZOrder, indexedEndZ, z);
		while(index < indexedSize) {
			int stackIndex = endZOrder[index++];
			if(indexedEndZ[stackIndex] != z) {
				break;
			}
			Placement candidate = indexedPlacements[stackIndex];
			if(!point.intersectsXY(candidate)) {
				continue;
			}
			pointSupporters.add(candidate);
		}
	}

	public void populatePointSupportees(Point point, int minDz, int maxDz) {
		pointSupportees.clear();
		int limitMinDz = point.getMinZ() + minDz;
		int limitMaxDz = point.getMinZ() + maxDz;
		List<Placement> placements = stack.getPlacements();
		if(placements.size() < MIN_INDEXED_STACK_SIZE) {
			for(int i = 0; i < placements.size(); i++) {
				Placement candidate = placements.get(i);
				int z = candidate.getAbsoluteZ();
				if(z >= limitMinDz && z <= limitMaxDz && point.intersectsXY(candidate)) {
					pointSupportees.add(candidate);
				}
			}
			return;
		}

		synchronizeStackIndex(placements);
		int candidateCount = 0;
		int index = lowerBound(minZOrder, indexedMinZ, limitMinDz);
		while(index < indexedSize) {
			int stackIndex = minZOrder[index++];
			if(indexedMinZ[stackIndex] > limitMaxDz) {
				break;
			}
			Placement candidate = indexedPlacements[stackIndex];
			if(!point.intersectsXY(candidate)) {
				continue;
			}
			candidateIndexes[candidateCount++] = stackIndex;
		}

		// Preserve stack insertion order so comparator tie handling and floating-point
		// accumulation stay deterministic when the requested band spans Z levels.
		if(limitMinDz != limitMaxDz) {
			Arrays.sort(candidateIndexes, 0, candidateCount);
		}
		for(int i = 0; i < candidateCount; i++) {
			pointSupportees.add(indexedPlacements[candidateIndexes[i]]);
		}
	}

	private void ensureIndexCapacity(int requiredCapacity) {
		if(indexedPlacements != null && requiredCapacity <= indexedPlacements.length) {
			return;
		}
		int oldCapacity = indexedPlacements == null ? 0 : indexedPlacements.length;
		int capacity = Math.max(requiredCapacity, oldCapacity + 16);
		indexedPlacements = indexedPlacements == null ? new Placement[capacity] : Arrays.copyOf(indexedPlacements, capacity);
		indexedMinZ = indexedMinZ == null ? new int[capacity] : Arrays.copyOf(indexedMinZ, capacity);
		indexedEndZ = indexedEndZ == null ? new int[capacity] : Arrays.copyOf(indexedEndZ, capacity);
		minZOrder = minZOrder == null ? new int[capacity] : Arrays.copyOf(minZOrder, capacity);
		endZOrder = endZOrder == null ? new int[capacity] : Arrays.copyOf(endZOrder, capacity);
		candidateIndexes = candidateIndexes == null ? new int[capacity] : Arrays.copyOf(candidateIndexes, capacity);
	}

	private void synchronizeStackIndex(List<Placement> placements) {
		int stackSize = placements.size();
		if(stackSize == indexedSize) {
			if(stackSize == 0) {
				return;
			}
			Placement tail = placements.get(stackSize - 1);
			if(isIndexedPlacementUnchanged(stackSize - 1, tail)) {
				return;
			}
		}

		ensureIndexCapacity(stackSize);

		int commonSize = Math.min(stackSize, indexedSize);
		if(commonSize > 0 && !isIndexedPlacementUnchanged(commonSize - 1, placements.get(commonSize - 1))) {
			commonSize = 0;
			while(commonSize < stackSize && commonSize < indexedSize
					&& isIndexedPlacementUnchanged(commonSize, placements.get(commonSize))) {
				commonSize++;
			}
		}

		while(indexedSize > commonSize) {
			removeLastIndexedPlacement();
		}
		while(indexedSize < stackSize) {
			addIndexedPlacement(placements.get(indexedSize));
		}
	}

	private boolean isIndexedPlacementUnchanged(int index, Placement placement) {
		return indexedPlacements[index] == placement && indexedMinZ[index] == placement.getAbsoluteZ()
				&& indexedEndZ[index] == placement.getAbsoluteEndZ();
	}

	private void addIndexedPlacement(Placement placement) {
		int stackIndex = indexedSize;
		indexedPlacements[stackIndex] = placement;
		indexedMinZ[stackIndex] = placement.getAbsoluteZ();
		indexedEndZ[stackIndex] = placement.getAbsoluteEndZ();

		insertOrdered(minZOrder, indexedMinZ, stackIndex);
		insertOrdered(endZOrder, indexedEndZ, stackIndex);
		indexedSize++;
	}

	private void removeLastIndexedPlacement() {
		int stackIndex = indexedSize - 1;
		removeOrdered(minZOrder, indexedMinZ, stackIndex);
		removeOrdered(endZOrder, indexedEndZ, stackIndex);
		indexedPlacements[stackIndex] = null;
		indexedSize--;
	}

	private void insertOrdered(int[] order, int[] values, int stackIndex) {
		int low = 0;
		int high = indexedSize;
		int value = values[stackIndex];
		while(low < high) {
			int middle = (low + high) >>> 1;
			if(values[order[middle]] <= value) {
				low = middle + 1;
			} else {
				high = middle;
			}
		}
		System.arraycopy(order, low, order, low + 1, indexedSize - low);
		order[low] = stackIndex;
	}

	private void removeOrdered(int[] order, int[] values, int stackIndex) {
		int index = lowerBound(order, values, values[stackIndex]);
		while(order[index] != stackIndex) {
			index++;
		}
		System.arraycopy(order, index + 1, order, index, indexedSize - index - 1);
	}

	private int lowerBound(int[] order, int[] values, int value) {
		int low = 0;
		int high = indexedSize;
		while(low < high) {
			int middle = (low + high) >>> 1;
			if(values[order[middle]] < value) {
				low = middle + 1;
			} else {
				high = middle;
			}
		}
		return low;
	}

	// =========================================================================
	// Shared instance helpers
	// =========================================================================

	protected void calculateRelifWeight(Placement placement, double reliefWeight) {
		long supportedArea = placement.getSupportedArea();
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			Placement supporter = placementLoad.getPlacement();
			double r = reliefWeight * placementLoad.getArea() / supportedArea;
			this.reliefWeights[supporter.getIndex()] += r;
			calculateRelifWeight(supporter, r);
		}
	}

	protected boolean isWithinMaxLoadWeightAndPressure(Placement placement, double weight, long area) {
		double effectiveWeight = weight - reliefWeights[placement.getIndex()];
		BoxStackValue sv = placement.getStackValue();
		if (sv.isMaxLoadPressure() && Box.calculatePressure(area, effectiveWeight) > sv.getMaxLoadPressure()) {
			return false;
		}
		if (sv.isMaxLoadWeight()) {
			double existingWeight = 0.0;
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
				double weightShare = effectiveWeight * pl.getArea() / totalArea;
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
	public long calculateSupportAndValidateSupporterLoad(BoxStackValue stackValue, int absoluteX, int absoluteY, double weight) {
		int n = placementSupporters.size();
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;
		long totalOverlapArea = 0;
		for (int i = 0; i < n; i++) {
			placementAreas[i] = LoadPlacementUtility.overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placementSupporters.get(i));
			totalOverlapArea += placementAreas[i];
		}
		for (int i = 0; i < n; i++) {
			double weightShare = weight * placementAreas[i] / totalOverlapArea;
			if (!isWithinMaxLoadWeightAndPressure(placementSupporters.get(i), weightShare, placementAreas[i])) {
				return -1;
			}
		}
		return totalOverlapArea;
	}

	public double calculateSupporteeWeight(BoxStackValue sv, Point point) {
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
	public abstract double calculateSupporteeLoad(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY);

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
		return populateSupporters(sv, minX, minY, point.getMinZ(), minX + sv.getDx() - 1, minY + sv.getDy() - 1);
	}

	/**
	 * Populates {@link #placementSupporters} for the given bounding box.
	 *
	 * @return {@code false} if any supporter constraint is violated
	 */
	public abstract boolean populateSupporters(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY);

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
		long supportedArea = getSupportedAreaAtPoint(point, sv, fullSupport);
		if(supportedArea == -1L) {
			return null;
		}

		Placement placement = acquirePlacement();
		placement.setStackValue(sv);
		placement.setPoint(point);
		placement.setSupportedArea(supportedArea);
		return placement;
	}

	@Override
	public long getSupportedAreaAtPoint(Point point, BoxStackValue sv, boolean fullSupport) {
		double weight = calculateSupporteeWeight(sv, point);
		if (weight == -1.0) {
			return -1L;
		}

		long supportedArea;
		if (point.getMinZ() > 0) {
			if (!populateSupporters(sv, point)) {
				return -1L;
			}
			supportedArea = calculateSupportAndValidateSupporterLoad(sv, point.getMinX(), point.getMinY(), weight);
			if (supportedArea == -1L) {
				return -1L;
			}
			if (fullSupport && supportedArea != sv.getArea()) {
				return -1L;
			}
		} else {
			supportedArea = sv.getArea();
		}

		return supportedArea;
	}

	@Override
	public void addSupportersLoad(Placement placement) {
		long totalArea = placement.getSupportedArea();
		if(placement.getAbsoluteZ() == 0 || totalArea == 0) {
			return;
		}
		placement.setSupportedArea(0);
		for(int i = 0; i < placementSupporters.size(); i++) {
			long area = placementAreas[i];
			placementSupporters.get(i).addLoad(placement, area, (double) placement.getWeight() * area / totalArea);
		}
	}

	@Override
	public void accepted(Placement placement) {
		if(placement.getAbsoluteZ() == 0) {
			return;
		}

		placementSupporters.clear();
		int supportZ = placement.getAbsoluteZ() - 1;
		int minX = placement.getAbsoluteX();
		int maxX = placement.getAbsoluteEndX();
		int minY = placement.getAbsoluteY();
		int maxY = placement.getAbsoluteEndY();

		long totalArea = 0L;
		List<Placement> placements = stack.getPlacements();
		if(placements.size() < MIN_INDEXED_STACK_SIZE) {
			for(int i = 0; i < placements.size(); i++) {
				Placement candidate = placements.get(i);
				if(candidate.getAbsoluteEndZ() != supportZ || !candidate.intersects2D(minX, maxX, minY, maxY)) {
					continue;
				}
				long area = LoadPlacementUtility.overlapArea(minX, minY, maxX, maxY, candidate);
				placementAreas[placementSupporters.size()] = area;
				placementSupporters.add(candidate);
				totalArea += area;
			}
			addAcceptedSupporterLoads(placement, totalArea);
			return;
		}

		synchronizeStackIndex(placements);
		int index = lowerBound(endZOrder, indexedEndZ, supportZ);
		while(index < indexedSize) {
			int stackIndex = endZOrder[index++];
			if(indexedEndZ[stackIndex] != supportZ) {
				break;
			}
			Placement candidate = indexedPlacements[stackIndex];
			if(!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			long area = LoadPlacementUtility.overlapArea(minX, minY, maxX, maxY, candidate);
			placementAreas[placementSupporters.size()] = area;
			placementSupporters.add(candidate);
			totalArea += area;
		}

		addAcceptedSupporterLoads(placement, totalArea);
	}

	private void addAcceptedSupporterLoads(Placement placement, long totalArea) {
		if(totalArea != 0L) {
			placement.setSupportedArea(0L);
			double weight = placement.getWeight();
			for(int i = 0; i < placementSupporters.size(); i++) {
				long area = placementAreas[i];
				placementSupporters.get(i).addLoad(placement, area, weight * area / totalArea);
			}
		}
	}

	/**
	 * Attempts to place {@code sv} at an inner position derived from {@code candidate}
	 * (full-support fallback).  Only accepts fully-supported results.
	 *
	 * @return a valid {@link Placement}, or {@code null} if any constraint fails
	 */
	public Placement getPlacementAtCandidate(Point point3d, BoxStackValue stackValue, Placement candidate, int limitX, int limitY, int z, int limitMaxX, int limitMaxY) {
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

		double weight = calculateSupporteeLoad(stackValue, x, y, point3d.getMinZ(), maxX, maxY);
		if (weight == -1.0) {
			return null;
		}

		if (!populateSupporters(stackValue, x, y, point3d.getMinZ(), maxX, maxY)) {
			return null;
		}

		long supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, x, y, weight);
		if (supportedArea < 0 || supportedArea != stackValue.getArea()) {
			return null;
		}

		Placement placement = acquirePlacement();
		placement.setStackValue(stackValue);
		placement.setPoint(point3d.getIndex(), x, y, point3d.getMinZ());
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
				recyclablePlacement = p;
				continue;
			}
			if(best != null) {
				recyclablePlacement = best;
			}
			best = p;
		}
		return best;
	}

	private Placement acquirePlacement() {
		Placement placement = recyclablePlacement;
		if(placement == null) {
			return new Placement();
		}

		recyclablePlacement = null;
		placement.clearLoad();
		placement.setProperties(null);
		placement.setIndex(0);
		return placement;
	}
}
