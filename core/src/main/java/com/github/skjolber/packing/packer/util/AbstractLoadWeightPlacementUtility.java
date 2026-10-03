package com.github.skjolber.packing.packer.util;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.PlacementList;
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

	// scratch for validating a candidate: new weight flowing through each reachable placement (by index)
	private double[] flowWeights;
	private boolean[] flowReached;
	private Placement[] flowPlacements;
	private int flowSize;

	// relief weight entries in use (by index), so that only those need to be reset
	private int[] reliefTouched;
	private boolean[] reliefTouchedMark;
	private int reliefTouchedSize;
	// scratch for spreading relief: placements to process and their pending share
	private Placement[] reliefQueue;
	private double[] reliefPending;
	private boolean[] reliefQueued;

	// scratch for box count checks: the depth each placement (by index) was checked at, in the current check
	private int[] visitDepth;
	private int[] visitStamp;
	private int stamp;

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
		flowWeights = new double[capacity];
		flowReached = new boolean[capacity];
		flowPlacements = new Placement[capacity];
		flowSize = 0;
		reliefTouched = new int[capacity];
		reliefTouchedMark = new boolean[capacity];
		reliefTouchedSize = 0;
		reliefQueue = new Placement[capacity];
		reliefPending = new double[capacity];
		reliefQueued = new boolean[capacity];
		visitDepth = new int[capacity];
		visitStamp = new int[capacity];
		stamp = 0;
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

	private void nextStamp() {
		stamp++;
		if(stamp == 0) {
			Arrays.fill(visitStamp, 0);
			stamp = 1;
		}
	}

	/**
	 * Same as {@link Placement#isWithinMaxLoadBoxCount(int)}: whether, for every path down the support
	 * graph, each placement at distance {@code i} allows at least {@code levels + i} boxes on top.
	 * <p>
	 * Each placement is checked once per depth instead of once per path: a placement reached again at
	 * the same or a lower depth has already been checked against a stricter limit, together with
	 * everything below it.
	 */
	protected boolean isWithinMaxLoadBoxCount(Placement placement, int levels) {
		nextStamp();
		return isWithinMaxLoadBoxCountVisit(placement, levels);
	}

	private boolean isWithinMaxLoadBoxCountVisit(Placement placement, int levels) {
		// only a placement with several supportees can be reached through several paths
		if(placement.getSupportees().size() > 1) {
			int index = placement.getIndex();
			if(visitStamp[index] == stamp && visitDepth[index] >= levels) {
				return true;
			}
			visitStamp[index] = stamp;
			visitDepth[index] = levels;
		}

		BoxStackValue sv = placement.getStackValue();
		if(sv.isMaxLoadBoxCount() && sv.getMaxLoadBoxCount() < levels) {
			return false;
		}
		List<PlacementLoad> supporters = placement.getSupporters();
		for (int i = 0; i < supporters.size(); i++) {
			if(!isWithinMaxLoadBoxCountVisit(supporters.get(i).getPlacement(), levels + 1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether every path up the support graph from {@code candidate} (including it) has fewer than
	 * {@code count} placements, i.e. at most {@code count} boxes would rest on a box below the candidate.
	 * <p>
	 * Each placement is checked once per remaining count instead of once per path: a placement reached
	 * again with the same or a higher remaining count has already been checked against a stricter limit.
	 */
	protected boolean isWithinSupporteeBoxCount(Placement candidate, int count) {
		nextStamp();
		return isWithinSupporteeBoxCountVisit(candidate, count);
	}

	private boolean isWithinSupporteeBoxCountVisit(Placement placement, int count) {
		if (count <= 0) {
			return false;
		}
		// only a placement with several supporters can be reached through several paths
		if(placement.getSupporters().size() > 1) {
			int index = placement.getIndex();
			// visitDepth holds the remaining count the placement was checked with; lower is stricter
			if(visitStamp[index] == stamp && visitDepth[index] <= count) {
				return true;
			}
			visitStamp[index] = stamp;
			visitDepth[index] = count;
		}

		List<PlacementLoad> supportees = placement.getSupportees();
		for (int k = 0; k < supportees.size(); k++) {
			if (!isWithinSupporteeBoxCountVisit(supportees.get(k).getPlacement(), count - 1)) {
				return false;
			}
		}
		return true;
	}

	/** Reset the relief weights from the previous candidate. */
	protected void resetReliefWeights() {
		for (int i = 0; i < reliefTouchedSize; i++) {
			int index = reliefTouched[i];
			reliefWeights[index] = 0;
			reliefTouchedMark[index] = false;
		}
		reliefTouchedSize = 0;
	}

	/**
	 * Add relief for {@code reliefWeight} which no longer rests on the supporters of {@code placement}
	 * (it is shifted onto the candidate), spread down the support graph by contact area.
	 * <p>
	 * The relief is spread top-down, once per placement rather than once per path: a placement can
	 * be reached through several paths, and walking every path grows exponentially with the height.
	 */
	protected void calculateRelifWeight(Placement placement, double reliefWeight) {
		int queueSize = 0;
		queueSize = spreadRelief(placement, reliefWeight, queueSize);

		int processed = 0;
		while (processed < queueSize) {
			// supporters lie strictly below their supportees: process the highest placement first,
			// so that all of its share has arrived
			int highest = processed;
			for (int i = processed + 1; i < queueSize; i++) {
				if(reliefQueue[i].getAbsoluteZ() > reliefQueue[highest].getAbsoluteZ()) {
					highest = i;
				}
			}
			Placement supporter = reliefQueue[highest];
			reliefQueue[highest] = reliefQueue[processed];
			reliefQueue[processed] = null;
			processed++;

			int index = supporter.getIndex();
			double relief = reliefPending[index];
			reliefPending[index] = 0;
			reliefQueued[index] = false;

			if(!reliefTouchedMark[index]) {
				reliefTouchedMark[index] = true;
				reliefTouched[reliefTouchedSize++] = index;
			}
			reliefWeights[index] += relief;

			queueSize = spreadRelief(supporter, relief, queueSize);
		}
	}

	private int spreadRelief(Placement placement, double relief, int queueSize) {
		long supportedArea = placement.getSupportedArea();
		List<PlacementLoad> supporters = placement.getSupporters();
		for (int i = 0; i < supporters.size(); i++) {
			PlacementLoad placementLoad = supporters.get(i);
			Placement supporter = placementLoad.getPlacement();
			int index = supporter.getIndex();
			if(!reliefQueued[index]) {
				reliefQueued[index] = true;
				reliefQueue[queueSize++] = supporter;
			}
			reliefPending[index] += relief * placementLoad.getArea() / supportedArea;
		}
		return queueSize;
	}

	/**
	 * Calculates the total overlap area of all {@link #placementSupporters} and validates the load
	 * constraints of every placement below which would carry part of {@code weight}.
	 * <p>
	 * The weight is spread over the support graph by contact area, summing the shares arriving
	 * through different paths. For each reached placement, the new load is its current load
	 * ({@link Placement#getLoadWeight()}, the total weight above it) plus the new weight flowing
	 * through it, less its relief (weight above the candidate which is shifted onto the candidate,
	 * see {@link #calculateRelifWeight(Placement, double)}). Weight limits are checked per placement,
	 * pressure limits per contact.
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
		try {
			for (int i = 0; i < n; i++) {
				Placement supporter = placementSupporters.get(i);
				double weightShare = weight * placementAreas[i] / totalOverlapArea;
				// a new contact carries no existing load
				if (!isWithinMaxLoadPressure(supporter, placementAreas[i], weightShare)) {
					return -1;
				}
				addFlow(supporter, weightShare);
			}
			if(!validateFlow()) {
				return -1;
			}
			return totalOverlapArea;
		} finally {
			clearFlow();
		}
	}

	private static boolean isWithinMaxLoadPressure(Placement placement, long area, double contactWeight) {
		BoxStackValue sv = placement.getStackValue();
		return !sv.isMaxLoadPressure() || Box.calculatePressure(area, contactWeight) <= sv.getMaxLoadPressure();
	}

	private void addFlow(Placement placement, double weight) {
		int index = placement.getIndex();
		if(!flowReached[index]) {
			flowReached[index] = true;
			flowPlacements[flowSize++] = placement;
		}
		flowWeights[index] += weight;
	}

	/**
	 * Process reached placements top-down, so that all the weight arriving at a placement is known
	 * before it is checked and passed on: a supporter always lies strictly below its supportee.
	 */
	private boolean validateFlow() {
		int processed = 0;
		while (processed < flowSize) {
			// pick the highest remaining placement
			int highest = processed;
			for (int i = processed + 1; i < flowSize; i++) {
				if(flowPlacements[i].getAbsoluteZ() > flowPlacements[highest].getAbsoluteZ()) {
					highest = i;
				}
			}
			Placement placement = flowPlacements[highest];
			flowPlacements[highest] = flowPlacements[processed];
			flowPlacements[processed] = placement;
			processed++;

			int index = placement.getIndex();
			double flow = flowWeights[index];
			double net = flow - reliefWeights[index];

			BoxStackValue sv = placement.getStackValue();
			if (sv.isMaxLoadWeight() && placement.getLoadWeight() + net > sv.getMaxLoadWeight()) {
				return false;
			}

			long totalArea = placement.getSupportedArea();
			if (totalArea > 0) {
				// existing load passed down by this placement, shared by contact area
				double carried = placement.getWeight() + placement.getLoadWeight();
				for (PlacementLoad pl : placement.getSupporters()) {
					long area = pl.getArea();
					if (!isWithinMaxLoadPressure(pl.getPlacement(), area, (carried + net) * area / totalArea)) {
						return false;
					}
					addFlow(pl.getPlacement(), flow * area / totalArea);
				}
			}
		}
		return true;
	}

	private void clearFlow() {
		for (int i = 0; i < flowSize; i++) {
			int index = flowPlacements[i].getIndex();
			flowWeights[index] = 0.0;
			flowReached[index] = false;
			flowPlacements[i] = null;
		}
		flowSize = 0;
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
		// the last start which keeps the box within the point
		int limitX = point3d.getMaxX() - stackValue.getDx() + 1;
		int limitY = point3d.getMaxY() - stackValue.getDy() + 1;
		// a supporter must at least reach the end of the box placed at the point origin
		int limitMaxX = point3d.getMinX() + stackValue.getDx() - 1;
		int limitMaxY = point3d.getMinY() + stackValue.getDy() - 1;

		if (z < 0) {
			// on the floor: always fully supported at the point origin
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
