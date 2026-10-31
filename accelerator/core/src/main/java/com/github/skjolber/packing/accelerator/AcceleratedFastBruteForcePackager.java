package com.github.skjolber.packing.accelerator;


import java.util.concurrent.ExecutorService;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator;
import com.github.skjolber.packing.packer.bruteforce.FastPointCalculator3DStack;

/**
 * The fast brute force packager with a {@link BruteForceAccelerator}, which searches the box orders of a container on threads ({@link FlatEngine}) or
 * on a device: the accelerator picks the best box order, and this packager places that order itself and confirms it, so the placements are the ones of
 * {@link FastBruteForcePackager}. How the accelerator is used is the {@link AcceleratorMode}: by default alongside this packager's own search, after a
 * gate. The accelerator applies to the containers whose search is the one it runs (see {@link #acceleratorProblem}); the others are packed as without
 * one.
 *
 * <pre>
 * AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(FlatEngine.newBuilder().build()).build();
 * </pre>
 */
public class AcceleratedFastBruteForcePackager extends FastBruteForcePackager {

	/** the gate of {@link AcceleratorMode#AUTO}: the packager searches a container alone for this long before the accelerator joins */
	public static final long DEFAULT_ACCELERATOR_GATE_MILLIS = 25L;

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends FastBruteForcePackager.Builder {

		protected BruteForceAccelerator accelerator;
		protected AcceleratorMode acceleratorMode = AcceleratorMode.AUTO;
		protected long acceleratorGateMillis = DEFAULT_ACCELERATOR_GATE_MILLIS;

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
		 * @param mode how the accelerator is used: by default {@link AcceleratorMode#AUTO}, the packager's own search with the accelerator alongside after
		 *        a gate ({@link #withAcceleratorGate(long)})
		 * @return this builder
		 */
		public Builder withAcceleratorMode(AcceleratorMode mode) {
			this.acceleratorMode = mode;
			return this;
		}

		/**
		 * @param millis in {@link AcceleratorMode#AUTO}, the time the packager searches a container alone before the accelerator joins: about what the
		 *        accelerator needs for any search, so that the containers which the packager finishes within it never reach the accelerator
		 * @return this builder
		 */
		public Builder withAcceleratorGate(long millis) {
			if(millis < 0L) {
				throw new IllegalArgumentException("Expected a gate of zero or more milliseconds, got " + millis);
			}
			this.acceleratorGateMillis = millis;
			return this;
		}

		@Override
		public Builder withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
			super.withIntermediatePackagerResultComparator(comparator);
			return this;
		}

		@Override
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			super.withRequireFullSupport(requireFullSupport);
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
		public Builder withThreads(int threads) {
			super.withThreads(threads);
			return this;
		}

		@Override
		public Builder withExecutorService(ExecutorService executorService, int threads) {
			super.withExecutorService(executorService, threads);
			return this;
		}

		@Override
		public Builder withPointComparator(FastBruteForceBoxStackValuePointComparator pointComparator) {
			super.withPointComparator(pointComparator);
			return this;
		}

		@Override
		protected FastBruteForcePackager newPackager(IntermediatePackagerResultComparator comparator, FastBruteForceBoxStackValuePointComparator pointComparator) {
			AcceleratedFastBruteForcePackager packager = new AcceleratedFastBruteForcePackager(comparator, pointComparator);
			if(accelerator != null) {
				packager.driver = new AcceleratorDriver<>(accelerator, acceleratorMode, acceleratorGateMillis * 1_000_000L);
			}
			return packager;
		}

		@Override
		public AcceleratedFastBruteForcePackager build() {
			return (AcceleratedFastBruteForcePackager)super.build();
		}
	}

	/** the accelerator and how it is used, or null for none */
	private AcceleratorDriver<BruteForceIntermediatePackagerResult> driver;

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

	public AcceleratedFastBruteForcePackager(IntermediatePackagerResultComparator comparator, FastBruteForceBoxStackValuePointComparator pointComparator) {
		super(comparator, pointComparator);
	}

	@Override
	protected BruteForceIntermediatePackagerResult packContainer(FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem,
			int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		AcceleratorDriver<BruteForceIntermediatePackagerResult> driver = this.driver;
		if(driver != null && driver.isActive()) {
			FlatProblem problem = acceleratorProblem(driver.getAccelerator(), containerItem, iterator, best, limit);
			if(problem != null) {
				BruteForceIntermediatePackagerResult result = driver.search(problem, getMinLoadVolume(best), interrupt,
						own -> pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, own, fastPointComparator, best, limit),
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
	 * @return the problem of the container for the accelerator, or null if the accelerator does not apply to it, or nothing can load more than the best
	 *         result ({@link #pack} returns at once then)
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
		FlatProblem problem = FlatProblem.of(iterator, holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz(), holder.getMaxLoadVolume(), holder.getMaxLoadWeight(), limit, anyBest);
		return accelerator.supports(problem) ? problem : null;
	}

	/**
	 * Place the accelerator's winning permutation (the positions are those of this packager) and compare with the claim.
	 *
	 * @return the result, or null if the claim was not confirmed (a bug in the accelerator): then pack the container with {@link #pack}
	 */
	private BruteForceIntermediatePackagerResult confirmAccelerated(AcceleratorWinner winner, FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount,
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
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
			PrefixBest prefix = new PrefixBest(length);
			searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, null, fastPointComparator, false, getInsertAfterCounts(iterator), maxPackableCount,
					null, Integer.MAX_VALUE, null, prefix);
			if(!isConfirmed(winner, prefix)) {
				return null;
			}
			result.setState(prefix.toList(), getState(iterator, prefix.getRotations(), prefix.getCount()), stackPlacements, stackPlacementCount);
			return result;
		} finally {
			// the packager searches the permutations from the first (and the other attempts reset the iterator themselves)
			iterator.reset();
		}
	}

	private static boolean isConfirmed(AcceleratorWinner winner, PrefixBest prefix) {
		int count = prefix.getCount();
		if(count != winner.getCount() || prefix.getLoadVolume() != winner.getLoadVolume()) {
			return false;
		}
		int[] rotations = winner.getRotations();
		int[] prefixRotations = prefix.getRotations();
		for (int i = 0; i < count; i++) {
			if(prefixRotations[i] != rotations[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if the search of the packager for the iterator's boxes is the one which the accelerator runs: see {@link #acceleratorProblem}
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
}
