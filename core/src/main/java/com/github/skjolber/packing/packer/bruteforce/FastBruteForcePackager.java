package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement.
 * So it does not try all possible placements (as i not all points)-
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public class FastBruteForcePackager extends AbstractBruteForcePackager {

	@FunctionalInterface
	public interface FastBruteForceBoxStackValuePointComparator {

		int compare(BoxStackValue stackValue, Point bestPoint, Point candidatePoint);
		
	}

	public static class DefaultFastBruteForceBoxStackValuePointComparator implements FastBruteForceBoxStackValuePointComparator {

		@Override
		public int compare(BoxStackValue stackValue, Point bestPoint, Point candidatePoint) {
			if(bestPoint.getArea() < candidatePoint.getArea()) {
				return -1;
			}
			if(bestPoint.getMinZ() < candidatePoint.getMinZ()) {
				return -1;
			}
			if(bestPoint.getArea() == candidatePoint.getArea() && bestPoint.getVolume() < candidatePoint.getVolume()) {
				return -1;
			}
			return 1;
		}
	}

	protected static final FastBruteForceBoxStackValuePointComparator DEFAULT_POINT_COMPARATOR = new DefaultFastBruteForceBoxStackValuePointComparator();

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected IntermediatePackagerResultComparator comparator;
		protected FastBruteForceBoxStackValuePointComparator pointComparator = DEFAULT_POINT_COMPARATOR;
		protected ContainerStrategyFactory containerStrategyFactory;
		
		public Builder withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
			this.comparator = comparator;
			return this;
		}

		protected BoxItemGroupComparator boxItemGroupComparator;
		protected boolean requireFullSupport;

		/**
		 * Place boxes only where they rest completely on the floor or on the boxes below: at the free points, and
		 * shifted from a free point onto the corner of a box below (as the plain packager's full support). Boxes do not
		 * rest on obstacles.
		 *
		 * @param requireFullSupport true to require full support
		 * @return this builder
		 */
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			this.requireFullSupport = requireFullSupport;
			return this;
		}

		/**
		 * Set the comparator which picks the order of box item groups with the same container priority and extraction
		 * order (by default the largest group first, like the plain packager).
		 *
		 * @param comparator box item group comparator
		 * @return this builder
		 */
		public Builder withBoxItemGroupComparator(BoxItemGroupComparator comparator) {
			this.boxItemGroupComparator = comparator;
			return this;
		}

		/**
		 * Set the factory which selects the container strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container strategy factory
		 * @return this builder
		 */
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public Builder withPointComparator(FastBruteForceBoxStackValuePointComparator pointComparator) {
			this.pointComparator = pointComparator;
			return this;
		}

		public FastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			
			FastBruteForcePackager packager = new FastBruteForcePackager(comparator, pointComparator);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			if(boxItemGroupComparator != null) {
				packager.setBoxItemGroupComparator(boxItemGroupComparator);
			}
			packager.setRequireFullSupport(requireFullSupport);
			return packager;
		}
		
	}
	
	private class FastBruteForceSession extends AbstractSingleThreadedBruteForceBoxItemSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceSession(List<BoxItem> boxItems, List<ContainerItem> containers,
				int containerCount, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, containers, containerCount, containerIterators, interrupt, hasLoadLimits(boxItems));
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceSession(FastBruteForceSession source) {
			super(source, source.load);
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		@Override
		protected FastBruteForceSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemSession(copyInitialBoxItems(), containers, containerCount, interrupt);
		}

		@Override
		public FastBruteForceSession fork() {
			return new FastBruteForceSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			containerIterators[i].reset();
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
				return packInOrderSkipping(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator,
						null, getMaxContainerPriority(containerIterators[i]), best);
			}
			if(order != Order.NONE) {
				return packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, getLimit(containerIterators[i]));
			}
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, getLimit(containerIterators[i]));
		}
		
	}
	
	private class FastBruteForceGroupSession extends AbstractSingleThreadedBruteForceBoxItemGroupSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containers, int containerCount,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, containers, containerCount, containerIterators, interrupt, hasLoadLimits(boxItems));
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceGroupSession(FastBruteForceGroupSession source) {
			super(source, source.load);
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		@Override
		protected FastBruteForceGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), containers, containerCount, interrupt);
		}

		@Override
		public FastBruteForceGroupSession fork() {
			return new FastBruteForceGroupSession(this);
		}

		
		@Override
		protected BruteForceIntermediatePackagerResult packGroupOrder(int containerIndex, BoxItemPermutationRotationIterator iterator, int[] groupOrder, IntermediatePackagerResult best) throws PackagerInterruptedException {
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(containerIndex), containerIndex, iterator, interrupt, fastPointComparator, best);
		}

		@Override
		protected IntermediatePackagerResultComparator getIntermediatePackagerResultComparator() {
			return intermediatePackagerResultComparator;
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			if(isGroupOrderSearch()) {
				return attemptGroupOrders(i, best);
			}
			BoxItemGroup[] iteratorGroups = containerIterators[i].getBoxItemGroups();
			// when skipping, the first group may be skipped
			if(order != Order.CHRONOLOGICAL_ALLOW_SKIPPING && !canLoadNextGroup(iteratorGroups)) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			containerIterators[i].reset();
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
				// groups are skipped whole
				return packInOrderSkipping(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator,
						getGroupSkipEnds(iteratorGroups, containerIterators[i].length()), getMaxContainerPriority(containerIterators[i]), best);
			}
			if(order != Order.NONE) {
				return truncateToGroup(packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, Integer.MAX_VALUE), iteratorGroups);
			}
			return truncateToGroup(FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best), iteratorGroups);
		}
		
	}

	@Override
	protected FastBruteForceGroupSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups,
			List<ContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt) {
		DefaultBoxItemGroupPermutationRotationIterator[] containerIterators = new DefaultBoxItemGroupPermutationRotationIterator[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			ContainerItem containerItem = containers.get(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		List<BoxItem> boxItems = new ArrayList<>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			boxItems.addAll(boxItemGroup.getItems());
		}
		return new FastBruteForceGroupSession(boxItems, itemGroups, containers, containerCount, containerIterators, interrupt);
	}

	@Override
	protected FastBruteForceSession createBoxItemSession(List<BoxItem> boxItems, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			ContainerItem containerItem = containers.get(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		return new FastBruteForceSession(boxItems, containers, containerCount, containerIterators, interrupt);
	}

	protected final FastBruteForceBoxStackValuePointComparator fastPointComparator;

	public FastBruteForcePackager(IntermediatePackagerResultComparator comparator, FastBruteForceBoxStackValuePointComparator pointComparator) {
		super(comparator);
		this.fastPointComparator = pointComparator;
	}

	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, pointComparator, null);
	}

	/**
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 */
	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator, IntermediatePackagerResult best) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, pointComparator, best, Integer.MAX_VALUE);
	}

	/**
	 * Pack the boxes of the iterator into one container: each permutation is searched as a box order (see
	 * {@link #searchOrder}), and the best result is kept.
	 *
	 * @param limit the number of leading boxes of the permutations which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 */
	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator, IntermediatePackagerResult best,
			int limit) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		// with box load limits, the placement tracks the loads of the placed boxes
		LoadPlacementUtility loadPlacementUtility = createLoadPlacementUtility(iterator, stack);

		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);
		if(limit == 0) {
			return bestResult;
		}
		if(loadPlacementUtility != null) {
			loadPlacementUtility.initialize(iterator.length());
		}
		// box item groups: the boxes of earlier groups; the permutations keep the groups' positions
		int[] insertAfterCounts = getInsertAfterCounts(iterator);

		boolean allItemsFit = limit >= iterator.length() && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}

		PrefixBest prefix = new PrefixBest(iterator.length());
		boolean checkExtraction = hasExtractionOrders(iterator);
		permutations:
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			int maxPackableCount = allItemsFit ? iterator.length() : Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
			int size;
			if(!allItemsFit && prefersHigherLoadVolume && getLoadVolume(iterator, maxPackableCount) < Math.max(minLoadVolume, bestResult.getLoadVolume())) {
				// no rotation of this permutation can load more than the best result
				size = maxPackableCount;
			} else {
				pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
				if(containerItem.hasInitialPoints()) {
					pointCalculator.setPoints(containerItem.getInitialPoints());
					pointCalculator.clear();
				}
				prefix.reset();
				searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, loadPlacementUtility, pointComparator, checkExtraction, insertAfterCounts, maxPackableCount,
						null, Integer.MAX_VALUE, null, prefix);
				size = prefix.count;
				if(size > 0) {
					bestPermutationResult.setState(prefix.points, getState(iterator, prefix.rotations, size), stackPlacements, stackPlacementCount);
					if(size == iterator.length()) {
						return bestPermutationResult;
					}
					if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) < 0) {
						// switch the two results for one another
						BruteForceIntermediatePackagerResult tmp = bestResult;
						bestResult = bestPermutationResult;
						bestPermutationResult = tmp;
					}
				}
			}
			// the next permutation which changes the boxes up to the first which could not be placed
			if(iterator.nextPermutation(size) == -1) {
				break permutations;
			}
		} while (true);

		return bestResult;
	}

	/**
	 * Pack the boxes in the iterator's order (a box item order) into one container: the order is searched without
	 * skipping (see {@link #searchOrder}), and each box must be insertable after the boxes before it.
	 *
	 * @param best the best result so far, or null
	 * @param limit the number of leading boxes which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 */
	public BruteForceIntermediatePackagerResult packInOrder(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator,
			IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility loadPlacementUtility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);
		if(limit == 0) {
			return result;
		}
		// the prefix of the order which fits by volume and weight
		int maxPackableCount = Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getLoadVolume(iterator, maxPackableCount) < minLoadVolume) {
			// cannot load more than the best result
			return result;
		}
		if(loadPlacementUtility != null) {
			loadPlacementUtility.initialize(iterator.length());
		}
		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.hasInitialPoints()) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		PrefixBest prefix = new PrefixBest(iterator.length());
		searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, loadPlacementUtility, pointComparator, hasExtractionOrders(iterator), null, maxPackableCount,
				null, Integer.MAX_VALUE, null, prefix);
		if(prefix.count > 0) {
			result.setState(prefix.points, getState(iterator, prefix.rotations, prefix.count), stackPlacements, stackPlacementCount);
		}
		return result;
	}

	/**
	 * Pack the boxes in the iterator's order (a box item order, {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}) into one
	 * container: any box may be skipped (it waits for a later container), and the boxes which are placed are in the order,
	 * each insertable after the boxes before it. The order is searched with skipping (see {@link #searchOrder}).
	 *
	 * @param skipEnds for each box, the box to continue with when skipping it, or -1 if it cannot be skipped (box item
	 *        groups are skipped whole), or null if each box can be skipped
	 * @param maxContainerPriority the highest container priority which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getMaxContainerPriority(BoxItemPermutationRotationIterator)})
	 * @param best the best result so far, or null (see {@link AbstractBruteForcePackager#packInOrderSkipping})
	 * @return the result, or an empty result; the result may hold any of the boxes (see
	 *         {@link BruteForceIntermediatePackagerResult#isAnyRemaining()})
	 */
	public BruteForceIntermediatePackagerResult packInOrderSkipping(FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount,
			ContainerItem containerItem, int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt,
			FastBruteForceBoxStackValuePointComparator pointComparator, int[] skipEnds, int maxContainerPriority, IntermediatePackagerResult best) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, utility != null);
		result.setAnyRemaining(true);
		int length = iterator.length();
		if(length == 0 || stackPlacements.length == 0) {
			return result;
		}
		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.hasInitialPoints()) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(length);
		}
		SkippingBest arrangements = newSkippingBest(result, best, stackPlacements, stackPlacementCount);
		searchInOrderSkipping(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, utility, pointComparator, skipEnds, maxContainerPriority, arrangements);
		stack.clear();
		BruteForceIntermediatePackagerResult packed = arrangements.best;
		packed.markDirty();
		return packed;
	}

	/**
	 * Search a box order with skipping (see {@link #searchOrder}).
	 */
	protected void searchInOrderSkipping(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator,
			int[] skipEnds, int maxContainerPriority, SkippingBest best) throws PackagerInterruptedException {
		searchOrder(pointCalculator, placements, iterator, stack, container, interrupt, utility, pointComparator, hasExtractionOrders(iterator), null, iterator.length(), skipEnds,
				maxContainerPriority, best, null);
	}

	/**
	 * Depth-first search over the boxes of the iterator's permutation, in order: each box is tried in each of its
	 * rotations, at the best position for the rotation (by the point comparator), and, when skipping, skipped. The same
	 * search packs a box item order ({@link Order#CHRONOLOGICAL}, and with skipping
	 * {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}) and each permutation of the boxes ({@link Order#NONE}).
	 * <br>
	 * <br>
	 * Without skipping, an arrangement ends at the first box which cannot be placed: the arrangements are the prefixes of
	 * the order, and the longest is the best. With skipping, the best arrangement is chosen by the result comparator.
	 * <br>
	 * <br>
	 * A position is valid where the box fits a free point, is insertable after the boxes placed before it (see
	 * {@code insertAfterCounts}), is not blocked by obstacles, keeps the extraction order and, with load limits, is
	 * carried by the boxes below; with full support required, the positions are where the box is fully supported (see
	 * {@link FullSupportCandidates}). Levels are boxes, while the point calculator has a frame for each placed box.
	 *
	 * @param insertAfterCounts null if each box must be insertable after all the boxes placed before it (a box item
	 *        order), empty if not, otherwise for each level the number of boxes placed before it which it must be
	 *        insertable after (box item groups)
	 * @param maxPackableCount without skipping, the number of leading boxes which may be placed
	 * @param skipEnds with skipping: for each level, the level to continue with when skipping it, -1 if it cannot be
	 *        skipped, or null for the next level
	 * @param maxContainerPriority the highest container priority which may be placed
	 * @param skipping the best arrangement, when skipping, otherwise null
	 * @param prefix the best arrangement, when not skipping, otherwise null
	 */
	protected void searchOrder(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator,
			boolean checkExtraction, int[] insertAfterCounts, int maxPackableCount, int[] skipEnds, int maxContainerPriority, SkippingBest skipping, PrefixBest prefix) throws PackagerInterruptedException {
		FastSearchFrames frames = pointCalculator.getSearchFrames();
		boolean[] unplaced = frames.unplaced;
		int[] rotationIndexes = frames.rotationIndexes;
		int[] parents = frames.parents;
		int[] placedCounts = frames.placedCounts;
		long[] placedVolumes = frames.placedVolumes;
		int[] freeLoadWeights = frames.freeLoadWeights;
		int[] maxContainerPriorities = frames.maxContainerPriorities;
		int[] placedPermutations = frames.placedPermutations;
		int[] placedRotations = frames.placedRotations;
		long[] remainingVolumes = frames.remainingVolumes;
		long[] minAreas = frames.minAreas;
		Box[] boxes = frames.boxes;

		int length = skipping != null ? iterator.length() : maxPackableCount;
		long maxLoadVolume = container.getMaxLoadVolume();
		boolean checkObstacles = !container.getObstacles().isEmpty();
		FullSupportCandidates candidates = requireFullSupport ? pointCalculator.getFullSupportCandidates() : null;

		// the box of each level, the smallest area of the boxes from each level on (in any rotation), and with skipping
		// (as a bound) their volume
		long minArea = Long.MAX_VALUE;
		for (int i = length - 1; i >= 0; i--) {
			Box box = iterator.getStackValue(i).getBox();
			boxes[i] = box;
			if(box.getMinimumArea() < minArea) {
				minArea = box.getMinimumArea();
			}
			minAreas[i] = minArea;
		}
		if(skipping != null) {
			long remainingVolume = 0L;
			remainingVolumes[length] = 0L;
			for (int i = length - 1; i >= 0; i--) {
				remainingVolume += boxes[i].getVolume();
				remainingVolumes[i] = remainingVolume;
			}
		}
		if(length == 0) {
			return;
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[0], iterator.getMinBoxVolume(0));

		int level = 0;
		parents[0] = -1;
		placedCounts[0] = 0;
		placedVolumes[0] = 0L;
		freeLoadWeights[0] = container.getMaxLoadWeight();
		maxContainerPriorities[0] = maxContainerPriority;
		boolean descend = true;
		while(true) {
			if(descend) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int placedCount = placedCounts[level];
				long placedVolume = placedVolumes[level];
				if(level == length) {
					// every box is placed or skipped: keep the arrangement if the best so far
					if(skipping != null) {
						if(placedCount > 0 && skipping.canBeBetter(placedVolume)) {
							skipping.offer(pointCalculator.getPoints(), placedPermutations, placedRotations, placedCount);
						}
					} else if(placedCount > prefix.count) {
						prefix.offer(pointCalculator.getPoints(), placedRotations, placedCount);
					}
					level = parents[level];
					descend = false;
					continue;
				}
				if(skipping != null && !skipping.canBeBetter(placedVolume + remainingVolumes[level])) {
					// cannot beat the best arrangement
					level = parents[level];
					descend = false;
					if(level == -1) {
						return;
					}
					continue;
				}
				Box box = boxes[level];
				// without skipping, the boxes up to the max packable count fit by volume and weight
				unplaced[level] = skipping != null && !(box.getBoxItem().getContainerPriority() <= maxContainerPriorities[level]
						&& box.getWeight() <= freeLoadWeights[level]
						&& placedVolume + box.getVolume() <= maxLoadVolume);
				rotationIndexes[level] = 0;
			} else if(!unplaced[level]) {
				// back from the next level: remove this level's placement and restore the free points
				int placedCount = placedCounts[level];
				if(utility != null) {
					Placement placement = placements[placedCount];
					placement.removeSupporteesAbove();
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				pointCalculator.setStackSize(placedCount);
				if((skipping != null ? skipping.count : prefix.count) == length) {
					// the best arrangement places all boxes: unwind without trying more candidates
					level = parents[level];
					if(level == -1) {
						return;
					}
					continue;
				}
			} else {
				// back from skipping this level's box
				level = parents[level];
				if(level == -1) {
					return;
				}
				continue;
			}

			int placedCount = placedCounts[level];
			if(!unplaced[level]) {
				// the next rotation of this level's box which has a valid position
				int insertAfterCount = insertAfterCounts == null ? placedCount : insertAfterCounts.length == 0 ? 0 : insertAfterCounts[level];
				BoxStackValue[] stackValues = boxes[level].getStackValues();
				BoxStackValue stackValue = null;
				int pointIndex = -1;
				SimplePoint3D point = null;
				while(pointIndex == -1 && rotationIndexes[level] < stackValues.length) {
					stackValue = stackValues[rotationIndexes[level]];
					rotationIndexes[level]++;
					if(candidates != null) {
						candidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
						int candidate = getBestFullySupported(candidates, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
						if(candidate != -1) {
							pointIndex = candidates.getPointIndex(candidate);
							point = candidates.getPoint(candidate);
						}
					} else {
						if(utility == null) {
							pointIndex = getBestPoint(pointCalculator, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, pointComparator);
						} else {
							pointIndex = getBestPointWithLoad(pointCalculator, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
						}
						if(pointIndex != -1) {
							point = pointCalculator.get(pointIndex);
						}
					}
				}
				if(pointIndex != -1) {
					// place the box and continue with the next level
					Placement placement = placements[placedCount];
					if(utility != null) {
						// the utility caches are primed for the last position checked: repopulate them for the selected one
						utility.populatePointSupporters(point);
						utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
						placement.clearLoad();
						placement.setIndex(stack.size());
						placement.setSupportedArea(utility.getSupportedAreaAtPoint(point, stackValue, false));
					}
					placement.setStackValue(stackValue);
					placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
					pointCalculator.add(pointIndex, placement, point);
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					int nextLevel = level + 1;
					if(nextLevel < length) {
						if(minAreas[nextLevel] != minAreas[level]) {
							pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[nextLevel], iterator.getMinBoxVolume(nextLevel));
						} else {
							pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
						}
					}
					placedPermutations[placedCount] = boxes[level].getBoxItem().getLocalIndex();
					placedRotations[placedCount] = rotationIndexes[level] - 1;
					enter(nextLevel, level, placedCount + 1, placedVolumes[level] + stackValue.getBox().getVolume(), freeLoadWeights[level] - stackValue.getBox().getWeight(),
							maxContainerPriorities[level], parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
					level = nextLevel;
					descend = true;
					continue;
				}
				unplaced[level] = true;
			}

			if(skipping == null) {
				// without skipping, the arrangement ends at this box
				if(placedCount > prefix.count) {
					prefix.offer(pointCalculator.getPoints(), placedRotations, placedCount);
				}
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}

			// skip this level's box: it waits for a later container, and so do the boxes of a higher container priority
			int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
			if(skipEnd == -1) {
				// cannot skip here (inside a box item group)
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}
			int skippedContainerPriority = Math.min(maxContainerPriorities[level], boxes[level].getBoxItem().getContainerPriority());
			enter(skipEnd, level, placedCount, placedVolumes[level], freeLoadWeights[level], skippedContainerPriority,
					parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
			level = skipEnd;
			descend = true;
		}
	}

	/**
	 * @param insertAfterCount the number of boxes placed before the box, which it must be insertable after
	 * @return the best position (by the point comparator) where the box is fully supported, insertable and, with load
	 *         limits, carried by the boxes below; or -1 if none
	 */
	protected int getBestFullySupported(FullSupportCandidates candidates, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
			boolean checkObstacles, boolean checkExtraction, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator) {
		int best = -1;
		for(int i = 0; i < candidates.size(); i++) {
			SimplePoint3D point = candidates.getPoint(i);
			if(insertAfterCount > 0 && !isInsertableAfter(point, stackValue, stack, insertAfterCount, container.getAccess())) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(best != -1 && pointComparator.compare(stackValue, candidates.getPoint(best), point) <= 0) {
				continue;
			}
			if(utility != null) {
				// validate the load only for positions which would be selected
				utility.populatePointSupporters(point);
				utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
				if(utility.getSupportedAreaAtPoint(point, stackValue, false) == -1L) {
					continue;
				}
			}
			best = i;
		}
		return best;
	}

	/**
	 * @param insertAfterCount the number of boxes placed before the box, which it must be insertable after
	 * @return the best point (by the point comparator) where the box fits and is insertable, or -1 if none
	 */
	protected int getBestPoint(FastPointCalculator3DStack pointCalculator, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
			boolean checkObstacles, boolean checkExtraction, FastBruteForceBoxStackValuePointComparator pointComparator) {
		int bestPointIndex = -1;
		for(int k = 0; k < pointCalculator.size(); k++) {
			SimplePoint3D point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(insertAfterCount > 0 && !isInsertableAfter(point, stackValue, stack, insertAfterCount, container.getAccess())) {
				continue;
			}
			if(bestPointIndex == -1 || pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), point) > 0) {
				bestPointIndex = k;
			}
		}
		return bestPointIndex;
	}

	/**
	 * As {@link #getBestPoint}, for boxes with load limits: the boxes below must carry the box at the point.
	 */
	protected int getBestPointWithLoad(FastPointCalculator3DStack pointCalculator, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
			boolean checkObstacles, boolean checkExtraction, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator) {
		int bestPointIndex = -1;
		for(int k = 0; k < pointCalculator.size(); k++) {
			SimplePoint3D point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(insertAfterCount > 0 && !isInsertableAfter(point, stackValue, stack, insertAfterCount, container.getAccess())) {
				continue;
			}
			if(bestPointIndex != -1 && pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), point) <= 0) {
				continue;
			}
			// validate the load only for points which would be selected
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			if(utility.getSupportedAreaAtPoint(point, stackValue, false) == -1L) {
				continue;
			}
			bestPointIndex = k;
		}
		return bestPointIndex;
	}
}
