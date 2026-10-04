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
		return null;
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
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
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

		// iterator over all permutations
		boolean allItemsFit = canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// iterate over all rotations
			bestPermutationResult.reset();
			int maxPackableCount = allItemsFit ? iterator.length() : getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
			if(!allItemsFit && prefersHigherLoadVolume && getLoadVolume(iterator, maxPackableCount) < Math.max(minLoadVolume, bestResult.getLoadVolume())) {
				// no rotation of this permutation can load more than the best result
				if(iterator.nextPermutation(maxPackableCount) == -1) {
					break;
				}
				continue;
			}

			do {
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

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
		search(pointCalculator, placements, iterator, stack, maxLoadWeight, interrupt, minStackableAreaIndex, maxPackableCount, null, pointFilter);
		return pointCalculator.getBestPoints();
	}

	/**
	 * Depth-first search for the deepest arrangement of the permutation's boxes. Level {@code i} places box
	 * {@code i} at each candidate point in turn, then continues with box {@code i + 1}; the point calculator
	 * keeps the deepest arrangement ({@link PointCalculator3DStack#getBestPoints()}). The search stops once
	 * {@code maxPackableCount} boxes are placed.
	 *
	 * <pre>
	 *  level 0:  point a ─┬─ level 1: point c ─── level 2: ...
	 *                     └─ level 1: point d ─── level 2: ...
	 *            point b ─── level 1: ...
	 * </pre>
	 *
	 * The state of each level is kept in {@link BruteForceSearchFrames}, so the number of boxes is not
	 * limited by the thread's stack.
	 *
	 * @param maxLoadWeight the container's max load weight
	 * @param utility load constraints, or null if none
	 * @param pointFilter candidate points, or null for all fitting points
	 * @throws PackagerInterruptedException if interrupted
	 */
	protected static void search(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, int maxLoadWeight,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter)
			throws PackagerInterruptedException {
		BruteForceSearchFrames frames = pointCalculator.getSearchFrames();
		int[] nextPointIndexes = frames.nextPointIndexes;
		int[] pointCounts = frames.pointCounts;
		IntIterator[] pointIterators = frames.pointIterators;
		int[] minStackableAreaIndexes = frames.minStackableAreaIndexes;
		int[] freeLoadWeights = frames.freeLoadWeights;

		minStackableAreaIndexes[0] = minStackableAreaIndex;
		freeLoadWeights[0] = maxLoadWeight;

		int level = 0;
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
				pointCalculator.updateBest();
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
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
					pointCalculator.pop();
					if(level == 0) {
						return;
					}
					level--;
					continue;
				}
				pointCalculator.redo();
			}

			// place the box at the next candidate point
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
				if(utility != null) {
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

			placement.setPoint(pointCalculator.get(pointIndex));
			if(utility != null) {
				placement.setIndex(stack.size());
				placement.setSupportedArea(supportedArea);
			}
			pointCalculator.add(pointIndex, placement);
			if(level + 1 >= maxPackableCount) {
				// all packable boxes are placed
				pointCalculator.updateBest();
				pointCalculator.pop();
				if(level == 0) {
					return;
				}
				level--;
				descend = false;
				continue;
			}

			stack.add(placement);
			if(utility != null) {
				utility.addSupportersLoad(placement);
			}
			int nextLevel = level + 1;
			int levelMinStackableAreaIndex = minStackableAreaIndexes[level];
			if(level == levelMinStackableAreaIndex) {
				int nextMinStackableAreaIndex = iterator.getMinStackableAreaIndex(nextLevel);
				pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(nextMinStackableAreaIndex).getArea(), iterator.getMinBoxVolume(nextLevel));
				minStackableAreaIndexes[nextLevel] = nextMinStackableAreaIndex;
			} else {
				pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
				minStackableAreaIndexes[nextLevel] = levelMinStackableAreaIndex;
			}
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
