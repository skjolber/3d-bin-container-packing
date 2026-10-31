package com.github.skjolber.packing.accelerator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.PointCalculator3DStack;

/**
 * The brute force packager (every box in every rotation at every free point where it fits) with a {@link BruteForceAccelerator}, which searches the
 * box orders of a container on threads or on a device: the accelerator picks the best box order, this packager places that order itself and confirms
 * it, so the placements are the ones of {@link BruteForcePackager}. The accelerator searches with all points ({@link FlatProblem#ALL_POINTS}), which
 * only the tree search of an accelerator does ({@link FlatTree}): a {@link FlatEngine} without {@link FlatEngine.Builder#withTree(boolean)} does not support
 * the problems of this packager, which then searches itself.
 * How the accelerator is used is the {@link AcceleratorMode}: by default alongside this packager's own search, after a gate. The accelerator applies
 * to the containers whose search is the one it runs (see {@link #acceleratorProblem}): no point filter, no full support, no box item groups, no load
 * limits, no obstacles, no initial points, no extraction orders, no container priorities, no reverse permutation skip, and the default comparators.
 */
public class AcceleratedBruteForcePackager extends BruteForcePackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends BruteForcePackager.Builder {

		protected BruteForceAccelerator accelerator;
		protected AcceleratorMode acceleratorMode = AcceleratorMode.AUTO;
		protected long acceleratorGateMillis = AcceleratedFastBruteForcePackager.DEFAULT_ACCELERATOR_GATE_MILLIS;
		protected boolean acceleratorAdaptiveGate = true;

		/**
		 * @param accelerator the accelerator, or null for none (the default); a {@link FlatEngine} needs {@link FlatEngine.Builder#withTree(boolean)}, see the class comment
		 * @return this builder
		 */
		public Builder withAccelerator(BruteForceAccelerator accelerator) {
			this.accelerator = accelerator;
			return this;
		}

		/**
		 * @param mode how the accelerator is used: by default {@link AcceleratorMode#AUTO}
		 * @return this builder
		 */
		public Builder withAcceleratorMode(AcceleratorMode mode) {
			this.acceleratorMode = mode;
			return this;
		}

		/**
		 * @param millis in {@link AcceleratorMode#AUTO}, the time the packager searches a container alone before the accelerator joins
		 * @return this builder
		 */
		public Builder withAcceleratorGate(long millis) {
			if(millis < 0L) {
				throw new IllegalArgumentException("Expected a gate of zero or more milliseconds, got " + millis);
			}
			this.acceleratorGateMillis = millis;
			return this;
		}

		/**
		 * @param adaptive in {@link AcceleratorMode#AUTO}, true (the default) if the gate of each box count adapts to the races: it doubles after a race
		 *        which the accelerator joined and lost, and halves after one it won, between the gate of {@link #withAcceleratorGate(long)} and
		 *        {@link AcceleratorDriver#GATE_CAP_FACTOR} times it. False keeps the gate as configured for every container.
		 * @return this builder
		 */
		public Builder withAcceleratorAdaptiveGate(boolean adaptive) {
			this.acceleratorAdaptiveGate = adaptive;
			return this;
		}

		@Override
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			super.withRequireFullSupport(requireFullSupport);
			return this;
		}

		@Override
		public Builder withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
			super.withIntermediatePackagerResultComparator(comparator);
			return this;
		}

		@Override
		public Builder withBoxItemGroupComparator(BoxItemGroupComparator comparator) {
			super.withBoxItemGroupComparator(comparator);
			return this;
		}

		@Override
		public Builder withContainerPackingStrategyFactory(ContainerPackingStrategyFactory factory) {
			super.withContainerPackingStrategyFactory(factory);
			return this;
		}

		@Override
		public Builder withTreeSearch(boolean treeSearch) {
			super.withTreeSearch(treeSearch);
			return this;
		}

		@Override
		public Builder withTreeSearchMinBoxes(int minBoxes) {
			super.withTreeSearchMinBoxes(minBoxes);
			return this;
		}

		@Override
		public Builder withTreeSearchAnyBest(boolean anyBest) {
			super.withTreeSearchAnyBest(anyBest);
			return this;
		}

		@Override
		public Builder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			super.withPointFilter(pointFilter);
			return this;
		}

		@Override
		public Builder withSkipReversePermutations(boolean filterReversePermutations) {
			super.withSkipReversePermutations(filterReversePermutations);
			return this;
		}

		@Override
		protected BruteForcePackager newPackager(IntermediatePackagerResultComparator comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
			AcceleratedBruteForcePackager packager = new AcceleratedBruteForcePackager(comparator, pointFilter, filterReversePermutations);
			if(accelerator != null) {
				packager.driver = new AcceleratorDriver<>(accelerator, acceleratorMode, acceleratorGateMillis * 1_000_000L, acceleratorAdaptiveGate);
			}
			return packager;
		}

		@Override
		public AcceleratedBruteForcePackager build() {
			return (AcceleratedBruteForcePackager)super.build();
		}
	}

	/** the accelerator and how it is used, or null for none */
	private AcceleratorDriver<BruteForceIntermediatePackagerResult> driver;

	public AcceleratedBruteForcePackager(IntermediatePackagerResultComparator comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
		super(comparator, pointFilter, filterReversePermutations);
	}

	/** @return the number of containers searched by the accelerator, with a result which the replay confirmed */
	public long getAcceleratedCount() {
		return driver == null ? 0L : driver.acceleratedCount.get();
	}

	/** @return the number of containers where the accelerator gave up, so the packager searched the permutations */
	public long getAcceleratorFallbackCount() {
		return driver == null ? 0L : driver.fallbackCount.get();
	}

	/** @return the number of containers where the winner of the accelerator was not confirmed by the replay, which is a bug in the accelerator */
	public long getAcceleratorMismatchCount() {
		return driver == null ? 0L : driver.mismatchCount.get();
	}

	/** @return the number of containers which the packager finished itself in {@link AcceleratorMode#AUTO}, before the accelerator joined or while it searched alongside */
	public long getAcceleratorRaceCpuCount() {
		return driver == null ? 0L : driver.raceCpuCount.get();
	}

	/**
	 * @param boxes a number of boxes
	 * @return the gate of {@link AcceleratorMode#AUTO} for the containers with that many boxes, in milliseconds: the configured gate, or what the races
	 *         of that box count made of it; zero without an accelerator
	 */
	public long getAcceleratorGateMillis(int boxes) {
		return driver == null ? 0L : driver.gateNanos(boxes) / 1_000_000L;
	}

	@Override
	protected BruteForceIntermediatePackagerResult packContainer(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem,
			int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		AcceleratorDriver<BruteForceIntermediatePackagerResult> driver = this.driver;
		if(driver != null && driver.isActive()) {
			FlatProblem problem = acceleratorProblem(driver.getAccelerator(), containerItem, iterator, best, limit);
			if(problem != null) {
				BruteForceIntermediatePackagerResult result = driver.search(problem, getMinLoadVolume(best), interrupt,
						own -> pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, own, pointFilter, best, limit),
						winner -> confirmAccelerated(winner, pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, limit));
				if(result != null) {
					return result;
				}
			}
		}
		return super.packContainer(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, best, limit);
	}

	@Override
	public void close() {
		super.close();
		if(driver != null) {
			driver.close();
		}
	}

	/**
	 * @return the problem of the container for the accelerator (all points), or null if the accelerator does not apply to it, or nothing can load
	 *         more than the best result ({@link #pack} returns at once then)
	 */
	private FlatProblem acceleratorProblem(BruteForceAccelerator accelerator, ContainerItem containerItem, BoxItemPermutationRotationIterator iterator, IntermediatePackagerResult best, int limit) {
		if(limit == 0 || !isAcceleratable(containerItem, iterator)) {
			return null;
		}
		int length = iterator.length();
		Container holder = containerItem.getContainer().copy(length);
		if(createLoadPlacementUtility(iterator, holder.getStack()) != null) {
			return null;
		}
		boolean allItemsFit = limit >= length && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return null;
		}
		// any arrangement of the best value will do when the search of this packager would be the any-best tree (below its minimum of boxes, or without
		// the tree, the loop keeps its first winning order): the accelerator may then prune ties
		boolean anyBest = treeSearchAnyBest && isTreeSearchable(containerItem, iterator, getInsertAfterCounts(iterator));
		FlatProblem problem = FlatProblem.of(iterator, holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz(), holder.getMaxLoadVolume(), holder.getMaxLoadWeight(), limit, anyBest, true);
		return accelerator.supports(problem) ? problem : null;
	}

	/**
	 * Place the accelerator's winning permutation (the positions are those of this packager) and compare with the claim.
	 *
	 * @return the result, or null if the claim was not confirmed (a bug in the accelerator): then pack the container with {@link #pack}
	 */
	private BruteForceIntermediatePackagerResult confirmAccelerated(AcceleratorWinner winner, PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount,
			ContainerItem containerItem, int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, int limit) throws PackagerInterruptedException {
		int length = iterator.length();
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(length), containerIndex, iterator, false);
		if(winner.getCount() == 0) {
			return result;
		}
		Container holder = containerItem.getContainer().copy(length);
		Stack stack = holder.getStack();
		boolean allItemsFit = limit >= length && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
		DefaultBoxItemPermutationRotationIterator defaultIterator = (DefaultBoxItemPermutationRotationIterator)iterator;
		try {
			defaultIterator.setPermutation(winner.getPermutation());
			int maxPackableCount = allItemsFit ? length : Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
			pointCalculator.resetBest();
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
			int[] rotations = new int[length];
			searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, null, null, false, false, getInsertAfterCounts(iterator), maxPackableCount, null,
					Integer.MAX_VALUE, null, rotations);
			stack.clear();
			List<Point> points = pointCalculator.getBestPoints();
			if(points.size() != winner.getCount()) {
				return null;
			}
			int[] claimed = winner.getRotations();
			for (int i = 0; i < points.size(); i++) {
				if(rotations[i] != claimed[i]) {
					return null;
				}
			}
			result.setStateFromReusablePoints(points, getState(iterator, rotations, points.size()), stackPlacements, stackPlacementCount);
			if(result.getLoadVolume() != winner.getLoadVolume() || result.getLoadWeight() != winner.getLoadWeight()) {
				return null;
			}
			return result;
		} finally {
			// the packager searches the permutations from the first (and the other attempts reset the iterator themselves)
			iterator.reset();
		}
	}

	/**
	 * @return true if the search of the packager for the iterator's boxes is the one which the accelerator runs: see the class comment
	 */
	private boolean isAcceleratable(ContainerItem containerItem, BoxItemPermutationRotationIterator iterator) {
		if(requireFullSupport || pointFilter != null || filterReversePermutations || !prefersHigherLoadVolume) {
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
}
