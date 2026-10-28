package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.bruteforce.flat.AcceleratorWinner;
import com.github.skjolber.packing.packer.bruteforce.flat.BruteForceAccelerator;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatEngine;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatProblem;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Packs each container by trying every box permutation and rotation, for each selecting the perceived best placement.
 * So it does not try all possible placements (i.e. not all points).
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
		protected ContainerPackingStrategyFactory containerPackingStrategyFactory;
		protected BruteForceAccelerator accelerator;
		protected boolean treeSearch = true;
		protected int treeSearchMinBoxes = DEFAULT_TREE_SEARCH_MIN_BOXES;
		
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
		 * Set the factory which selects the container packing strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container packing strategy factory
		 * @return this builder
		 */
		public Builder withContainerPackingStrategyFactory(ContainerPackingStrategyFactory factory) {
			this.containerPackingStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public Builder withPointComparator(FastBruteForceBoxStackValuePointComparator pointComparator) {
			this.pointComparator = pointComparator;
			return this;
		}

		/**
		 * Set an accelerator which searches the permutations of the boxes of a container (box order {@link Order#NONE}) instead of the packager. It is used
		 * where the search is the same as the one of the packager (see {@link BruteForceAccelerator}), and the result is the one which the packager would
		 * return: the accelerator picks the best permutation, and the packager places that permutation. Otherwise, and when the accelerator gives up, the
		 * packager searches the permutations itself.
		 *
		 * @param accelerator the accelerator, or null for none (the default)
		 * @return this builder
		 */
		public Builder withAccelerator(BruteForceAccelerator accelerator) {
			this.accelerator = accelerator;
			return this;
		}

		/**
		 * Search the box orders of a container as one tree ({@link com.github.skjolber.packing.packer.bruteforce.flat.FlatTreeItem}), on the calling thread,
		 * where no accelerator is set and the search is the one of the loop over the permutations (the conditions of {@link #withAccelerator}): every prefix
		 * of boxes is placed once and the tree prunes with bounds, which needs far fewer nodes than the loop. The result is the one of the loop. On by default.
		 *
		 * @param treeSearch false to search with the loop over the permutations only
		 * @return this builder
		 */
		public Builder withTreeSearch(boolean treeSearch) {
			this.treeSearch = treeSearch;
			return this;
		}

		/**
		 * @param minBoxes the fewest boxes of a container for the tree search; with fewer, the loop over the permutations is quicker. The default is
		 *        {@value #DEFAULT_TREE_SEARCH_MIN_BOXES}.
		 * @return this builder
		 */
		public Builder withTreeSearchMinBoxes(int minBoxes) {
			if(minBoxes < 0) {
				throw new IllegalArgumentException("Expected zero or more boxes, got " + minBoxes);
			}
			this.treeSearchMinBoxes = minBoxes;
			return this;
		}

		public FastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			
			FastBruteForcePackager packager = new FastBruteForcePackager(comparator, pointComparator);
			packager.accelerator = accelerator;
			packager.treeSearch = treeSearch;
			packager.treeSearchMinBoxes = treeSearchMinBoxes;
			if(containerPackingStrategyFactory != null) {
				packager.setContainerPackingStrategyFactory(containerPackingStrategyFactory);
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
			BruteForceIntermediatePackagerResult accelerated = packWithAccelerator(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt,
					best, getLimit(containerIterators[i]));
			if(accelerated != null) {
				return accelerated;
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
		protected BruteForceIntermediatePackagerResult packGroupOrder(int containerIndex, DefaultBoxItemGroupPermutationRotationIterator iterator, int[] groupOrder,
				IntermediatePackagerResult best) throws PackagerInterruptedException {
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

	/** the fewest boxes of a container for the tree search by default, see {@link Builder#withTreeSearchMinBoxes(int)} */
	public static final int DEFAULT_TREE_SEARCH_MIN_BOXES = 6;

	/** The accelerator which searches the permutations, or null, see {@link Builder#withAccelerator(BruteForceAccelerator)} */
	private BruteForceAccelerator accelerator;
	/** whether to search the box orders as a tree where no accelerator is set, see {@link Builder#withTreeSearch(boolean)} */
	private boolean treeSearch = true;
	private int treeSearchMinBoxes = DEFAULT_TREE_SEARCH_MIN_BOXES;
	/** the tree search on the calling thread, made when first needed */
	private volatile FlatEngine treeEngine;

	/** containers packed by the accelerated search: by the first order when all boxes fit, or by the accelerator with a result which the replay confirmed (statistics) */
	final AtomicLong acceleratedCount = new AtomicLong();
	/** containers where the accelerator gave up, so the packager searched the permutations */
	final AtomicLong acceleratorFallbackCount = new AtomicLong();
	/** containers where the winner of the accelerator was not confirmed by the replay: the packager searched the permutations; this is a bug */
	final AtomicLong acceleratorMismatchCount = new AtomicLong();

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
				// an arrangement with less load volume than the best result always compares worse: do not even build it
				if(size > 0 && (size == iterator.length() || !prefersHigherLoadVolume || bestResult.isEmpty() || prefix.loadVolume >= bestResult.getLoadVolume())) {
					bestPermutationResult.setState(prefix.toList(), getState(iterator, prefix.rotations, size), stackPlacements, stackPlacementCount);
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
	 * Pack the boxes of the iterator into one container as {@link #pack}, with the accelerator searching the permutations instead of this packager: the
	 * accelerator picks the best permutation, and this packager places that permutation, and confirms that the result is the one the accelerator found.
	 * <br>
	 * <br>
	 * The accelerator applies for box order {@link Order#NONE} without box item groups, load limits, obstacles, extraction orders, initial points, full
	 * support, container priorities, or another point comparator or result comparator than the defaults: for those the search is not the one which the
	 * accelerator runs.
	 *
	 * @param best the best result so far, or null
	 * @param limit see {@link #pack}
	 * @return the result, or null if there is no accelerator, or it does not apply, or it gave up: then pack the container with {@link #pack}
	 */
	protected BruteForceIntermediatePackagerResult packWithAccelerator(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		BruteForceAccelerator accelerator = this.accelerator;
		if(accelerator == null && treeSearch) {
			accelerator = treeEngine();
		}
		if(accelerator == null || limit == 0 || !isAcceleratable(containerItem, iterator)) {
			return null;
		}
		int length = iterator.length();
		Container holder = containerItem.getContainer().copy(length);
		Stack stack = holder.getStack();
		if(createLoadPlacementUtility(iterator, stack) != null) {
			return null;
		}
		boolean allItemsFit = limit >= length && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			// nothing can load more than the best result: pack(..) returns at once
			return null;
		}
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(length), containerIndex, iterator, false);
		FlatProblem problem = null;
		if(accelerator != treeEngine) {
			problem = FlatProblem.of(iterator, holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz(), holder.getMaxLoadVolume(), holder.getMaxLoadWeight(), limit);
			if(!accelerator.supports(problem)) {
				return null;
			}
		} else if(length < treeSearchMinBoxes) {
			return null;
		}
		if(allItemsFit) {
			// the first order wins outright if it places all boxes, as it does for the loop; that is one search, which no accelerator beats
			iterator.reset();
			try {
				pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
				PrefixBest prefix = new PrefixBest(length);
				searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, null, fastPointComparator, false, getInsertAfterCounts(iterator), length, null,
						Integer.MAX_VALUE, null, prefix);
				if(prefix.count == length) {
					result.setState(prefix.toList(), getState(iterator, prefix.rotations, prefix.count), stackPlacements, stackPlacementCount);
					acceleratedCount.incrementAndGet();
					return result;
				}
			} finally {
				iterator.reset();
			}
		}
		if(problem == null) {
			problem = FlatProblem.of(iterator, holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz(), holder.getMaxLoadVolume(), holder.getMaxLoadWeight(), limit);
		}
		Optional<AcceleratorWinner> found = accelerator.search(problem, minLoadVolume, interrupt);
		if(found.isEmpty()) {
			acceleratorFallbackCount.incrementAndGet();
			return null;
		}
		AcceleratorWinner winner = found.get();
		if(winner.getCount() == 0) {
			acceleratedCount.incrementAndGet();
			return result;
		}

		// the positions are those of this packager: place the winning permutation, and compare with the claim
		DefaultBoxItemPermutationRotationIterator defaultIterator = (DefaultBoxItemPermutationRotationIterator)iterator;
		try {
			defaultIterator.setPermutation(winner.getPermutation());
			int maxPackableCount = allItemsFit ? length : Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
			PrefixBest prefix = new PrefixBest(length);
			searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, null, fastPointComparator, false, getInsertAfterCounts(iterator), maxPackableCount,
					null, Integer.MAX_VALUE, null, prefix);
			if(!isConfirmed(winner, prefix)) {
				acceleratorMismatchCount.incrementAndGet();
				return null;
			}
			result.setState(prefix.toList(), getState(iterator, prefix.rotations, prefix.count), stackPlacements, stackPlacementCount);
			acceleratedCount.incrementAndGet();
			return result;
		} finally {
			// the packager searches the permutations from the first (and the other attempts reset the iterator themselves)
			iterator.reset();
		}
	}

	/** the tree search of this packager, on the calling thread */
	private FlatEngine treeEngine() {
		FlatEngine engine = treeEngine;
		if(engine == null) {
			synchronized (this) {
				engine = treeEngine;
				if(engine == null) {
					// one item: the whole tree, with the most prefixes shared; the interrupt is polled when the tree pauses
					engine = FlatEngine.newBuilder().withThreads(1).withItemsPerThread(1).withMinBoxes(treeSearchMinBoxes).build();
					treeEngine = engine;
				}
			}
		}
		return engine;
	}

	@Override
	public void close() {
		super.close();
		FlatEngine engine = treeEngine;
		if(engine != null) {
			engine.close();
		}
	}

	private static boolean isConfirmed(AcceleratorWinner winner, PrefixBest prefix) {
		if(prefix.count != winner.getCount() || prefix.loadVolume != winner.getLoadVolume()) {
			return false;
		}
		int[] rotations = winner.getRotations();
		for (int i = 0; i < prefix.count; i++) {
			if(prefix.rotations[i] != rotations[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if the search of the packager for the iterator's boxes is the one which the accelerator runs: see {@link #packWithAccelerator}
	 */
	private boolean isAcceleratable(ContainerItem containerItem, BoxItemPermutationRotationIterator iterator) {
		if(requireFullSupport || fastPointComparator != DEFAULT_POINT_COMPARATOR || !prefersHigherLoadVolume) {
			return false;
		}
		Class<?> comparatorClass = intermediatePackagerResultComparator.getClass();
		if(comparatorClass != BruteForceIntermediatePackagerResultComparator.class && comparatorClass != DefaultIntermediatePackagerResultComparator.class) {
			return false;
		}
		if(!(iterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator) || defaultIterator.hasBlocks()) {
			return false;
		}
		if(containerItem.hasInitialPoints() || !containerItem.getContainer().getObstacles().isEmpty()) {
			return false;
		}
		return !hasExtractionOrders(iterator) && getInsertAfterCounts(iterator).length == 0;
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
			result.setState(prefix.toList(), getState(iterator, prefix.rotations, prefix.count), stackPlacements, stackPlacementCount);
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
		BoxItem[] items = frames.items;
		BoxStackValue[][] stackValues = frames.stackValues;

		int length = skipping != null ? iterator.length() : maxPackableCount;
		long maxLoadVolume = container.getMaxLoadVolume();
		boolean checkObstacles = !container.getObstacles().isEmpty();
		FullSupportCandidates candidates = requireFullSupport ? pointCalculator.getFullSupportCandidates() : null;

		// the box of each level, the smallest area of the boxes from each level on (in any rotation), and with skipping
		// (as a bound) their volume
		long minArea = Long.MAX_VALUE;
		for (int i = length - 1; i >= 0; i--) {
			boxes[i] = iterator.getStackValue(i).getBox();
			items[i] = iterator.getBoxItem(i);
			BoxStackValue[] rotations = iterator.getStackValues(i);
			stackValues[i] = rotations;
			for (BoxStackValue rotation : rotations) {
				if(rotation.getArea() < minArea) {
					minArea = rotation.getArea();
				}
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
						prefix.offer(pointCalculator, placedRotations, placedCount, placedVolume);
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
				unplaced[level] = skipping != null && !(items[level].getContainerPriority() <= maxContainerPriorities[level]
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
				BoxStackValue[] levelStackValues = stackValues[level];
				BoxStackValue stackValue = null;
				int pointIndex = -1;
				SimplePoint3D point = null;
				while(pointIndex == -1 && rotationIndexes[level] < levelStackValues.length) {
					stackValue = levelStackValues[rotationIndexes[level]];
					rotationIndexes[level]++;
					if(candidates != null) {
						candidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
						int candidate = getBestFullySupported(candidates, items[level], stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
						if(candidate != -1) {
							pointIndex = candidates.getPointIndex(candidate);
							point = candidates.getPoint(candidate);
						}
					} else {
						if(utility == null) {
							pointIndex = getBestPoint(pointCalculator, items[level], stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, pointComparator);
						} else {
							pointIndex = getBestPointWithLoad(pointCalculator, items[level], stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
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
					placement.setStackValue(items[level], stackValue);
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
					placedPermutations[placedCount] = items[level].getLocalIndex();
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
					prefix.offer(pointCalculator, placedRotations, placedCount, placedVolumes[level]);
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
			int skippedContainerPriority = Math.min(maxContainerPriorities[level], items[level].getContainerPriority());
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
	protected int getBestFullySupported(FullSupportCandidates candidates, BoxItem boxItem, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
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
			if(checkExtraction && !isExtractable(point, boxItem, stackValue, stack, container.getAccess())) {
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
	protected int getBestPoint(FastPointCalculator3DStack pointCalculator, BoxItem boxItem, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
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
			if(checkExtraction && !isExtractable(point, boxItem, stackValue, stack, container.getAccess())) {
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
	protected int getBestPointWithLoad(FastPointCalculator3DStack pointCalculator, BoxItem boxItem, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
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
			if(checkExtraction && !isExtractable(point, boxItem, stackValue, stack, container.getAccess())) {
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
