package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * For one container, {@link #pack(PointCalculator3DStack, Placement[], int, ContainerItem, int, BoxItemPermutationRotationIterator, PackagerInterruptSupplier, BruteForcePointIteratorFilter, IntermediatePackagerResult)}
 * runs three nested searches:
 * <ol>
 * <li>every permutation (order) of the boxes,</li>
 * <li>for each permutation, every combination of rotations, and</li>
 * <li>for each permutation and rotation, every placement of the boxes in that order at the free (extreme) points,
 * see {@link #search(PointCalculator3DStack, Placement[], BoxItemPermutationRotationIterator, Stack, int, PackagerInterruptSupplier, int, int, LoadPlacementUtility, BruteForcePointIteratorFilter, List, ContainerAccess)}.</li>
 * </ol>
 * Boxes are always placed in permutation order, so a result is a prefix of a permutation: the longest prefix which could be placed.
 * This allows skipping work which cannot change the outcome:
 * <ul>
 * <li>if the first {@code n} boxes were placed, rotating box {@code n + 1} or later cannot place more boxes,
 * and neither can reordering them, so the iterators skip ahead to a change at index {@code n} or lower,</li>
 * <li>a prefix cannot be longer than the boxes which fit by volume and weight ({@link #getMaxPackableCount(BoxItemPermutationRotationIterator, long, long)}),
 * so the search stops once that many are placed, and</li>
 * <li>when the comparator prefers higher load volume, permutations (and containers) whose maximum load volume does not exceed
 * the best result so far are skipped.</li>
 * </ul>
 * Note: The search is exponential in the number of boxes. It is not intended for more than about 10 boxes per container.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public abstract class AbstractBruteForcePackager extends AbstractPackager<AbstractBruteForcePackager.BruteForcePackagerResultBuilder> {



	public AbstractBruteForcePackager(Comparator<IntermediatePackagerResult> comparator) {
		super(comparator);
	}
	
	public static class BruteForcePackagerResultBuilder extends AbstractPackagerResultBuilder<BruteForcePackagerResultBuilder> {
	
		private AbstractBruteForcePackager packager;
	
		public BruteForcePackagerResultBuilder withPackager(AbstractBruteForcePackager packager) {
			this.packager = packager;
			return this;
		}

		@Override
		public PackagerResult build() {
			return packager.pack(validate(packager), deadline, interrupt);
		}
	}

	protected boolean supportsLoad() {
		return false;
	}

	@Override
	public String getUnsupportedReason(PackagerInput input) {
		if(!supportsLoad() && input.hasBoxItems()) {
			for (BoxItem boxItem : input.getBoxItems()) {
				if(boxItem.isMaxLoad() || boxItem.getBox().isLoadIdenticalBoxOnly()) {
					return "Max load not supported for brute force packager";
				}
			}
		}
		for(ContainerItem container : input.getContainerItems()) {
			if(container.hasControls()) {
				return "Controls not supported";
			}
		}
		if(input.getOrder() != Order.NONE) {
			return "Order not supported for brute force packager";
		}
		if(hasContainerPriorities(input)) {
			return "Container priorities not supported for brute force packager";
		}
		return null;
	}

	private static boolean hasContainerPriorities(PackagerInput input) {
		if(input.hasBoxItems()) {
			List<BoxItem> boxItems = input.getBoxItems();
			for (int i = 1; i < boxItems.size(); i++) {
				if(boxItems.get(i).getContainerPriority() != boxItems.get(0).getContainerPriority()) {
					return true;
				}
			}
			return false;
		}
		List<BoxItemGroup> groups = input.getBoxItemGroups();
		for (int i = 1; i < groups.size(); i++) {
			if(groups.get(i).getContainerPriority() != groups.get(0).getContainerPriority()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		if(input.hasBoxItems()) {
			AbstractPackagerSession.initializeGlobalIndexes(input.getBoxItems());
			return createBoxItemSession(input.getBoxItems(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
		}
		AbstractPackagerSession.initializeGlobalIndexesForGroups(input.getBoxItemGroups());
		return createBoxItemGroupSession(input.getBoxItemGroups(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withPackager(this);
	}

	protected abstract AbstractBruteForceBoxItemSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt);

	protected abstract AbstractBruteForceBoxItemSession createBoxItemSession(List<BoxItem> items, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt);

	static Placement[] getPlacements(int size, boolean load) {
		// each box will at most have a single placement with a space (and its remainder).
		Placement[] placements = new Placement[size];

		for (int i = 0; i < size; i++) {
			placements[i] = new Placement(load);
		}
		return placements;
	}

	static Placement[] getPlacements(int size) {
		return getPlacements(size, false);
	}

	static int removeFirstPlacements(Placement[] placements, int size, int count) {
		int remaining = count - size;
		if(remaining > 0) {
			System.arraycopy(placements, size, placements, 0, remaining);
		}
		return remaining;
	}

	/**
	 * Returns an upper bound for the number of items in the current permutation
	 * which can be packed without exceeding the container's volume or weight.
	 *
	 * <p>
	 * The brute-force search packs a prefix of each permutation. Therefore,
	 * once the next item would exceed either capacity, no placement branch or
	 * rotation of that permutation can produce a longer result.
	 * </p>
	 */
	protected static int getMaxPackableCount(BoxItemPermutationRotationIterator iterator, long maxLoadVolume, long maxLoadWeight) {
		long loadVolume = 0L;
		long loadWeight = 0L;
		for (int i = 0; i < iterator.length(); i++) {
			BoxStackValue stackValue = iterator.getStackValue(i);
			long volume = stackValue.getBox().getVolume();
			long weight = stackValue.getBox().getWeight();
			if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
				return i;
			}
			loadVolume += volume;
			loadWeight += weight;
		}
		return iterator.length();
	}

	/**
	 * @return the load volume a result must exceed to be selected over {@code best}, or 0
	 */
	protected long getMinLoadVolume(IntermediatePackagerResult best) {
		if(best == null || !prefersHigherLoadVolume || best.isEmpty()) {
			return 0L;
		}
		return best.getLoadVolume();
	}

	/**
	 * @return an upper bound for the load volume of any result for the container
	 */
	protected static long getMaxLoadVolume(BoxItemPermutationRotationIterator iterator, Container holder, boolean allItemsFit) {
		long volume = getLoadVolume(iterator, iterator.length());
		if(allItemsFit) {
			return volume;
		}
		return Math.min(volume, holder.getMaxLoadVolume());
	}

	/**
	 * @return the volume of the first {@code count} boxes of the current permutation
	 */
	protected static long getLoadVolume(BoxItemPermutationRotationIterator iterator, int count) {
		long volume = 0L;
		for (int i = 0; i < count; i++) {
			volume += iterator.getStackValue(i).getBox().getVolume();
		}
		return volume;
	}

	protected static boolean canPackAll(BoxItemPermutationRotationIterator iterator, long maxLoadVolume, long maxLoadWeight) {
		long loadVolume = 0L;
		long loadWeight = 0L;
		for (int i = 0; i < iterator.length(); i++) {
			BoxStackValue stackValue = iterator.getStackValue(i);
			long volume = stackValue.getBox().getVolume();
			long weight = stackValue.getBox().getWeight();
			if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
				return false;
			}
			loadVolume += volume;
			loadWeight += weight;
		}
		return true;
	}

	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, index, iterator, interrupt, pointFilter, null);
	}

	/**
	 * Pack the boxes of the iterator into one container: the best placement over all permutations and rotations.
	 * Within a permutation, the result which places the most boxes wins (it is a longer prefix, so it has more volume and weight);
	 * across permutations, results are compared with the packager's comparator.
	 *
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 * @return the best result, or an empty result; the result refers to reused state (stack placements, points) until materialized
	 */
	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, IntermediatePackagerResult best)
			throws PackagerInterruptedException {

		Container holder = containerItem.getContainer().copy(iterator.length());
		
		Stack stack = holder.getStack();
		
		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, supportsLoad());
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, supportsLoad());

		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);

		// if all boxes fit by volume and weight, every permutation may place all of them;
		// otherwise each permutation is limited to the prefix which fits (see getMaxPackableCount(..))
		boolean allItemsFit = canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}
		// outer loop: permutations
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			bestPermutationResult.reset();
			int maxPackableCount = allItemsFit ? iterator.length() : getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
			if(!allItemsFit && prefersHigherLoadVolume && getLoadVolume(iterator, maxPackableCount) < Math.max(minLoadVolume, bestResult.getLoadVolume())) {
				// no rotation of this permutation can load more than the best result,
				// and neither can permutations which only reorder boxes after the packable prefix
				if(iterator.nextPermutation(maxPackableCount) == -1) {
					break;
				}
				continue;
			}

			// inner loop: rotations of the current permutation
			do {
				// the box with the smallest area; free points with less area cannot hold any of the boxes
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

				// place the boxes in permutation order, returns the points of the longest prefix placed

				List<Point> points = packStackPlacement(pointCalculator, stackPlacements, iterator, stack, holder, interrupt,
						minStackableAreaIndex, containerItem.getInitialPoints(), utility, pointFilter, maxPackableCount);
				stack.clear();
				
				if(points.size() > bestPermutationResult.getSize()) {
					bestPermutationResult.setStateFromReusablePoints(points, iterator.getState(), stackPlacements, stackPlacementCount);
					if(points.size() == iterator.length()) {
						// best possible result for this container
						return bestPermutationResult;
					}
				}
				if(points.size() >= maxPackableCount) {
					// Capacity makes a longer prefix impossible for every rotation
					// of this permutation.
					break;
				}

				// search for the next rotation which actually
				// has a chance of affecting the result.
				// i.e. if we have four boxes, and two boxes could be placed with the
				// current rotations, and the new rotation only changes the rotation of box 4,
				// then we know that attempting to stack again will not work

				int rotationIndex = iterator.nextRotation(points.size());

				if(rotationIndex == -1) {
					// no more rotations, continue to next permutation
					break;
				}
			} while (true);

			// the first bestPermutationResult.getSize() boxes were placed, but the next could not be:
			// skip permutations which keep the same boxes up to and including that index
			int permutationIndex = iterator.nextPermutation(bestPermutationResult.getSize());

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result
				
				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) < 0) {
					// switch the two results for one another
					BruteForceIntermediatePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}
			
			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		bestResult.markDirty();

		return bestResult;
	}

	protected abstract LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack);

	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points, LoadPlacementUtility loadPlacementUtility, BruteForcePointIteratorFilter pointFilter)
			throws PackagerInterruptedException {
		int maxPackableCount = placements.length == 0 ? 0 : getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
		return preservePoints(packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt,
				minStackableAreaIndex, points, loadPlacementUtility, pointFilter, maxPackableCount));
	}

	protected static List<Point> preservePoints(List<Point> points) {
		if(points.isEmpty()) {
			return Collections.emptyList();
		}
		ArrayList<Point> result = new ArrayList<>(points.size());
		for(int i = 0; i < points.size(); i++) {
			result.add(points.get(i));
		}
		return result;
	}

	protected List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points,
			LoadPlacementUtility loadPlacementUtility, BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
		pointCalculator.resetBest();
		if(placements.length == 0) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		int maxLoadWeight = container.getMaxLoadWeight();
		if(maxPackableCount == 0) {
			return Collections.emptyList();
		}

		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));
		search(pointCalculator, placements, iterator, stack, maxLoadWeight, interrupt, minStackableAreaIndex, maxPackableCount, null, pointFilter, container.getObstacles(), container.getAccess());
		return pointCalculator.getBestPoints();
	}

	/**
	 * Depth-first search for the longest prefix of the permutation which can be placed, with the current rotations.
	 * <br>
	 * <br>
	 * Level {@code i} of the search places box {@code i} of the permutation. Each level tries the box at each candidate
	 * point in turn; after a placement, the next level tries the next box at the free points which remain. When a level
	 * runs out of points, the search goes back to the previous level, which removes its placement and tries its next point:
	 *
	 * <pre>
	 *  level 0 (box 0)    level 1 (box 1)    level 2 (box 2)
	 *  point a ─────────┬─ point c ────────── point e, f, ...
	 *                   └─ point d ────────── ...
	 *  point b ─────────── point c ────────── ...
	 * </pre>
	 *
	 * The search is a loop over two steps rather than recursion, so the number of boxes is not limited by the thread's stack:
	 * <ul>
	 * <li><b>descend</b> into a level: record the boxes placed so far as the best arrangement if it is the longest yet,
	 * save the point calculator's state ({@link PointCalculator3DStack#push()}) and start at the level's first candidate point.</li>
	 * <li><b>ascend</b> back into a level from the next level: remove the level's placement from the stack (and its load links),
	 * and restore the free points to before the placement ({@link PointCalculator3DStack#redo()}), then continue with the next candidate point.</li>
	 * </ul>
	 * After either step, the box is placed at the next candidate point ({@link PointCalculator3DStack#add(int, Placement)}
	 * calculates the new free points) and the search descends to the next level. A level without more candidate points
	 * discards its saved state ({@link PointCalculator3DStack#pop()}) and ascends.
	 * <br>
	 * <br>
	 * The search ends when level 0 has no more points, or as soon as {@code maxPackableCount} boxes are placed; no arrangement
	 * can be longer than that. A level is skipped (the search ascends without trying any point) if its box is heavier than
	 * the remaining load weight, since then no box from that index on can be part of the prefix.
	 * <br>
	 * <br>
	 * The point calculator also discards free points which are too small for every remaining box: the minimum area is that of the
	 * smallest remaining box ({@code minStackableAreaIndex}), updated when that box has been placed, and the minimum volume is
	 * {@link BoxItemPermutationRotationIterator#getMinBoxVolume(int)}.
	 * <br>
	 * <br>
	 * The result is the longest arrangement found, as points per box, in {@link PointCalculator3DStack#getBestPoints()}
	 * ({@link PointCalculator3DStack#getBestStackIndex()} boxes). The state of each level is kept in {@link BruteForceSearchFrames}.
	 *
	 * @param placements one reusable placement per level
	 * @param stack the placed boxes, for load constraints; boxes are added on descend and removed on ascend
	 * @param maxLoadWeight the container's max load weight
	 * @param minStackableAreaIndex index of the box with the smallest area, from level 0
	 * @param maxPackableCount the maximum number of boxes which fit by volume and weight
	 * @param utility load constraints, or null if none
	 * @param pointFilter candidate points, or null for all fitting points
	 * @throws PackagerInterruptedException if interrupted
	 */
	/**
	 * @return true if a box can be inserted at the point after the boxes already in the container (obstacles):
	 *         none of them rests on it, or is in its path (see {@link ContainerAccess})
	 */
	protected static boolean isInsertable(Point point, BoxStackValue stackValue, List<Placement> obstacles, ContainerAccess access) {
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		int endZ = z + stackValue.getDz() - 1;
		for (int i = 0; i < obstacles.size(); i++) {
			if(obstacles.get(i).mustFollow(x, y, z, endX, endY, endZ, access)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if the boxes of the iterator have different extraction orders
	 */
	protected static boolean hasExtractionOrders(BoxItemPermutationRotationIterator iterator) {
		for (int i = 1; i < iterator.length(); i++) {
			if(iterator.getStackValue(i).getBox().getBoxItem().getExtractionOrder() != iterator.getStackValue(0).getBox().getBoxItem().getExtractionOrder()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if a box at the point does not prevent extracting the placed boxes in their extraction order, and
	 *         is not prevented by them: a box extracted earlier must not have a box extracted later resting on it, or
	 *         in its path (see {@link ContainerAccess})
	 */
	protected static boolean isExtractable(Point point, BoxStackValue stackValue, Stack stack, ContainerAccess access) {
		int order = stackValue.getBox().getBoxItem().getExtractionOrder();
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		int endZ = z + stackValue.getDz() - 1;
		List<Placement> placements = stack.getPlacements();
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			int placementOrder = placement.getBoxItem().getExtractionOrder();
			if(order < placementOrder) {
				if(placement.mustFollow(x, y, z, endX, endY, endZ, access)) {
					return false;
				}
			} else if(order > placementOrder) {
				if(placement.mustPrecede(x, y, z, endX, endY, endZ, access)) {
					return false;
				}
			}
		}
		return true;
	}

	protected static void search(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, int maxLoadWeight,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter,
			List<Placement> obstacles, ContainerAccess access)
			throws PackagerInterruptedException {
		boolean checkObstacles = obstacles != null && !obstacles.isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		BruteForceSearchFrames frames = pointCalculator.getSearchFrames();
		int[] nextPointIndexes = frames.nextPointIndexes;
		int[] pointCounts = frames.pointCounts;
		IntIterator[] pointIterators = frames.pointIterators;
		int[] minStackableAreaIndexes = frames.minStackableAreaIndexes;
		int[] freeLoadWeights = frames.freeLoadWeights;

		minStackableAreaIndexes[0] = minStackableAreaIndex;
		freeLoadWeights[0] = maxLoadWeight;

		// the current level, i.e. the index of the box being placed
		int level = 0;
		// true when entering the level from the previous level, false when coming back from the next level
		boolean descend = true;
		while(true) {
			Placement placement = placements[level];
			if(descend) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				BoxStackValue stackValue;
				if(pointCalculator.getBestStackIndex() >= maxPackableCount || (stackValue = iterator.getStackValue(level)).getBox().getWeight() > freeLoadWeights[level]) {
					// nothing to do at this level, continue at the previous level
					if(level == 0) {
						return;
					}
					level--;
					descend = false;
					continue;
				}
				placement.setStackValue(stackValue);
				// the boxes of levels 0 .. level - 1 are placed; keep them if the longest arrangement so far
				pointCalculator.updateBest();
				// save the free points, so that each candidate point of this level starts from them (see redo())
				pointCalculator.push();
				if(pointFilter == null) {
					pointCounts[level] = pointCalculator.size();
					nextPointIndexes[level] = 0;
				} else {
					pointIterators[level] = pointFilter.getPoints(pointCalculator, stackValue);
				}
			} else {
				// back from the next level: remove this level's placement
				if(utility != null) {
					placement.removeSupporteesAbove();
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
					// a longest possible arrangement was found below: unwind without trying more points
					pointCalculator.pop();
					if(level == 0) {
						return;
					}
					level--;
					continue;
				}
				// restore the free points to before this level's placement
				pointCalculator.redo();
			}

			// find the next candidate point for this level's box: a point which fits the box
			// (all points, or the point filter's points) and, with load constraints, where the box is supported
			// without overloading the boxes below
			BoxStackValue stackValue = placement.getStackValue();
			int pointIndex = -1;
			long supportedArea = 0L;
			while(true) {
				int candidate;
				if(pointFilter == null) {
					int k = nextPointIndexes[level];
					int count = pointCounts[level];
					while(k < count && !pointCalculator.get(k).fits3D(stackValue)) {
						k++;
					}
					if(k == count) {
						break;
					}
					nextPointIndexes[level] = k + 1;
					candidate = k;
				} else {
					IntIterator pointIterator = pointIterators[level];
					if(!pointIterator.hasNext()) {
						break;
					}
					candidate = pointIterator.next();
				}
				if(checkObstacles && !isInsertable(pointCalculator.get(candidate), stackValue, obstacles, access)) {
					// an obstacle rests on the box at this point, or is in its path
					continue;
				}
				if(checkExtraction && !isExtractable(pointCalculator.get(candidate), stackValue, stack, access)) {
					continue;
				}
				if(utility != null) {
					// -1 if the boxes below cannot carry the box at this point
					SimplePoint3D point = pointCalculator.get(candidate);
					utility.populatePointSupporters(point);
					utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
					supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
					if(supportedArea == -1L) {
						continue;
					}
				}
				pointIndex = candidate;
				break;
			}
			if(pointIndex == -1) {
				// no more points at this level
				pointCalculator.pop();
				if(level == 0) {
					return;
				}
				level--;
				descend = false;
				continue;
			}

			// place the box; add(..) replaces the free points with those around the new placement
			placement.setPoint(pointCalculator.get(pointIndex));
			if(utility != null) {
				placement.setIndex(stack.size());
				placement.setSupportedArea(supportedArea);
			}
			pointCalculator.add(pointIndex, placement);
			if(level + 1 >= maxPackableCount) {
				// all packable boxes are placed: record the arrangement, which ends the search (see the checks above)
				pointCalculator.updateBest();
				pointCalculator.pop();
				if(level == 0) {
					return;
				}
				level--;
				descend = false;
				continue;
			}

			// descend to the next box
			stack.add(placement);
			if(utility != null) {
				utility.addSupportersLoad(placement);
			}
			int nextLevel = level + 1;
			// the minimum area changes only when the smallest remaining box was the one just placed;
			// the minimum volume of the remaining boxes is cached per index by the iterator
			int levelMinStackableAreaIndex = minStackableAreaIndexes[level];
			if(level == levelMinStackableAreaIndex) {
				int nextMinStackableAreaIndex = iterator.getMinStackableAreaIndex(nextLevel);
				pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(nextMinStackableAreaIndex).getArea(), iterator.getMinBoxVolume(nextLevel));
				minStackableAreaIndexes[nextLevel] = nextMinStackableAreaIndex;
			} else {
				pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
				minStackableAreaIndexes[nextLevel] = levelMinStackableAreaIndex;
			}
			// the load weight left for the remaining boxes
			freeLoadWeights[nextLevel] = freeLoadWeights[level] - stackValue.getBox().getWeight();
			level = nextLevel;
			descend = true;
		}
	}

	protected boolean acceptAsFull(BruteForceIntermediatePackagerResult result, Container holder) {
		return result.getLoadVolume() == holder.getMaxLoadVolume();
	}
	
	@Override
	protected BruteForceIntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return BruteForceIntermediatePackagerResult.EMPTY;
	}
}
