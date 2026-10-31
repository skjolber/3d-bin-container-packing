package com.github.skjolber.packing.accelerator;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

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
 * gate. The accelerator applies to the containers whose search is the one it runs (see {@link #packWithAccelerator}); the others are packed as without
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
			packager.accelerator = accelerator;
			packager.acceleratorMode = acceleratorMode;
			packager.acceleratorGateNanos = acceleratorGateMillis * 1_000_000L;
			return packager;
		}

		@Override
		public AcceleratedFastBruteForcePackager build() {
			return (AcceleratedFastBruteForcePackager)super.build();
		}
	}

	/** The accelerator which searches the permutations, or null, see {@link Builder#withAccelerator(BruteForceAccelerator)} */
	private BruteForceAccelerator accelerator;
	private AcceleratorMode acceleratorMode = AcceleratorMode.AUTO;
	private long acceleratorGateNanos = DEFAULT_ACCELERATOR_GATE_MILLIS * 1_000_000L;
	/** the thread on which the accelerator searches alongside this packager in {@link AcceleratorMode#AUTO}, made when first needed */
	private ExecutorService acceleratorExecutor;
	/** containers which the packager finished itself while the accelerator searched alongside, or before it joined (statistics) */
	final AtomicLong acceleratorRaceCpuCount = new AtomicLong();

	/** containers searched by the accelerator, with a result which the replay confirmed (statistics) */
	final AtomicLong acceleratedCount = new AtomicLong();
	/** containers where the accelerator gave up, so the packager searched the permutations */
	final AtomicLong acceleratorFallbackCount = new AtomicLong();
	/** containers where the winner of the accelerator was not confirmed by the replay: the packager searched the permutations; this is a bug */
	final AtomicLong acceleratorMismatchCount = new AtomicLong();

	/** @return the number of containers searched by the accelerator, with a result which the replay confirmed */
	public long getAcceleratedCount() {
		return acceleratedCount.get();
	}

	/** @return the number of containers where the accelerator gave up, so the packager searched the permutations */
	public long getAcceleratorFallbackCount() {
		return acceleratorFallbackCount.get();
	}

	/** @return the number of containers where the winner of the accelerator was not confirmed by the replay, which is a bug in the accelerator */
	public long getAcceleratorMismatchCount() {
		return acceleratorMismatchCount.get();
	}

	/** @return the number of containers which the packager finished itself in {@link AcceleratorMode#AUTO}, before the accelerator joined or while it searched alongside */
	public long getAcceleratorRaceCpuCount() {
		return acceleratorRaceCpuCount.get();
	}

	public AcceleratedFastBruteForcePackager(IntermediatePackagerResultComparator comparator, FastBruteForceBoxStackValuePointComparator pointComparator) {
		super(comparator, pointComparator);
	}

	@Override
	protected BruteForceIntermediatePackagerResult packContainer(FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem,
			int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		if(accelerator != null && acceleratorMode == AcceleratorMode.GPU_ONLY) {
			BruteForceIntermediatePackagerResult accelerated = packWithAccelerator(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, best, limit);
			if(accelerated != null) {
				return accelerated;
			}
		} else if(accelerator != null && acceleratorMode == AcceleratorMode.AUTO) {
			BruteForceIntermediatePackagerResult raced = packRacing(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, best, limit);
			if(raced != null) {
				return raced;
			}
		}
		return super.packContainer(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, best, limit);
	}

	@Override
	public void close() {
		super.close();
		synchronized (this) {
			if(acceleratorExecutor != null) {
				acceleratorExecutor.shutdownNow();
				acceleratorExecutor = null;
			}
		}
	}

	private synchronized ExecutorService acceleratorExecutor() {
		if(acceleratorExecutor == null) {
			acceleratorExecutor = Executors.newCachedThreadPool(runnable -> {
				Thread thread = new Thread(runnable, "packing-accelerator");
				thread.setDaemon(true);
				return thread;
			});
		}
		return acceleratorExecutor;
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
		if(accelerator == null) {
			return null;
		}
		FlatProblem problem = acceleratorProblem(accelerator, containerItem, iterator, best, limit);
		if(problem == null) {
			return null;
		}
		Optional<AcceleratorWinner> found = accelerator.search(problem, getMinLoadVolume(best), interrupt);
		if(found.isEmpty()) {
			acceleratorFallbackCount.incrementAndGet();
			return null;
		}
		return confirmAccelerated(found.get(), pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, limit);
	}

	/**
	 * Pack the boxes of the iterator into one container as {@link #pack}, with the accelerator searching alongside ({@link AcceleratorMode#AUTO}):
	 * this packager starts at once, the accelerator after the gate, each stopping when the other has a result. The accelerator's winner is placed and
	 * confirmed as in {@link #packWithAccelerator}; when it is not confirmed, or the accelerator gave up, this packager searches on alone.
	 *
	 * @return the result, or null if the accelerator does not apply: then pack the container with {@link #pack}
	 */
	protected BruteForceIntermediatePackagerResult packRacing(FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem,
			int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, IntermediatePackagerResult best, int limit)
			throws PackagerInterruptedException {
		BruteForceAccelerator accelerator = this.accelerator;
		if(accelerator == null) {
			return null;
		}
		FlatProblem problem = acceleratorProblem(accelerator, containerItem, iterator, best, limit);
		if(problem == null) {
			return null;
		}
		final long minLoadVolume = getMinLoadVolume(best);
		final long gateNanos = acceleratorGateNanos;
		final AtomicBoolean cpuDone = new AtomicBoolean();
		final AtomicBoolean acceleratorDone = new AtomicBoolean();
		final PackagerInterruptSupplier acceleratorInterrupt = () -> cpuDone.get() || interrupt.getAsBoolean();
		Future<Optional<AcceleratorWinner>> future = acceleratorExecutor().submit(() -> {
			// the gate: the packager alone, unless it is already done
			long deadline = System.nanoTime() + gateNanos;
			while (System.nanoTime() < deadline) {
				if(cpuDone.get()) {
					return Optional.<AcceleratorWinner>empty();
				}
				LockSupport.parkNanos(200_000L);
			}
			if(cpuDone.get()) {
				return Optional.<AcceleratorWinner>empty();
			}
			Optional<AcceleratorWinner> found = accelerator.search(problem, minLoadVolume, acceleratorInterrupt);
			if(found.isPresent() && !cpuDone.get()) {
				// a result: the packager's own search stops
				acceleratorDone.set(true);
			}
			return found;
		});
		final PackagerInterruptSupplier cpuInterrupt = () -> acceleratorDone.get() || interrupt.getAsBoolean();
		try {
			BruteForceIntermediatePackagerResult result = pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, cpuInterrupt, fastPointComparator,
					best, limit);
			cpuDone.set(true);
			acceleratorRaceCpuCount.incrementAndGet();
			return result;
		} catch (PackagerInterruptedException e) {
			cpuDone.set(true);
			if(interrupt.getAsBoolean()) {
				throw e;
			}
			// the accelerator has a result
			Optional<AcceleratorWinner> found;
			try {
				found = future.get();
			} catch (InterruptedException | ExecutionException ex) {
				found = Optional.empty();
			}
			if(found.isPresent()) {
				BruteForceIntermediatePackagerResult result = confirmAccelerated(found.get(), pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt,
						limit);
				if(result != null) {
					return result;
				}
			} else {
				acceleratorFallbackCount.incrementAndGet();
			}
			// not confirmed, or gone: the packager searches alone, from the start
			iterator.reset();
			return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, fastPointComparator, best, limit);
		} finally {
			cpuDone.set(true);
			future.cancel(false);
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
		// any arrangement of the best value will do when the tree search of this packager would take any: the accelerator may then prune ties
		FlatProblem problem = FlatProblem.of(iterator, holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz(), holder.getMaxLoadVolume(), holder.getMaxLoadWeight(), limit, treeSearchAnyBest);
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
			acceleratedCount.incrementAndGet();
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
}
