package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerException;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.iterator.AbstractBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.FilteredReversedBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelBoxItemGroupPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.ParallelBoxItemPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Brute-force packager which searches on several threads.
 * <br>
 * <br>
 * Note on parallelization: the permutations of the boxes are split into different tasks, and for box item groups
 * without a box item order, the orders of the groups (by their first groups). The rotations and point placements of a
 * permutation are not split.
 */

public class ParallelBruteForcePackager extends AbstractBruteForcePackager {

	private static final class LocalInterrupt {
		private volatile boolean interrupted;
		/** The number of boxes of a result which cannot be beaten (the search is then complete), besides one which holds the last box */
		private final int completeBoxCount;

		private LocalInterrupt() {
			this(Integer.MAX_VALUE);
		}

		private LocalInterrupt(int completeBoxCount) {
			this.completeBoxCount = completeBoxCount;
		}
	}

	/** Thread priority marker: leave the threads' priority alone */
	private static final int NO_THREAD_PRIORITY = -1;

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private static final int MAX_BOUNDED_POOL_SIZE = 1 << 16;

		protected int threads = -1;
		protected int threadPriority = NO_THREAD_PRIORITY;
		protected int parallelizationCount = -1;
		protected ExecutorService executorService;
		protected IntermediatePackagerResultComparator comparator;
		protected BruteForcePointIteratorFilter pointFilter;
		protected boolean filterReversePermutations = false;
		protected ContainerPackingStrategyFactory containerPackingStrategyFactory;

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

		public Builder withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
			this.comparator = comparator;
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

		public Builder withThreads(int threads) {
			if(threads < 1) {
				throw new IllegalArgumentException("Unexpected thread count " + threads);
			}
			this.threads = threads;
			return this;
		}

		/**
		 * Search at a thread priority, for example a low one to leave the CPU to other work. By default, the threads' priority is left alone.
		 * <br>
		 * <br>
		 * The thread priority is a hint to the operating system's scheduler (it may be ignored), and clamped by the maximum priority of the thread group.
		 * Each packing task sets the priority of the thread which runs it, and restores the thread's original priority when the task is done; this includes
		 * the pool threads of a {@linkplain #withExecutorService(ExecutorService) supplied executor service}, and the thread which calls the packager.
		 * An executor service created by this builder also creates its threads at this priority.
		 *
		 * @param threadPriority thread priority, from {@linkplain Thread#MIN_PRIORITY} to {@linkplain Thread#MAX_PRIORITY}
		 * @return this builder
		 * @throws IllegalArgumentException if the priority is outside the range
		 */

		public Builder withThreadPriority(int threadPriority) {
			if(threadPriority < Thread.MIN_PRIORITY || threadPriority > Thread.MAX_PRIORITY) {
				throw new IllegalArgumentException("Unexpected thread priority " + threadPriority);
			}
			this.threadPriority = threadPriority;
			return this;
		}

		/**
		 * 
		 * Number of units to split the work into. This number should by an order of magnitude larger than the threads.
		 * 
		 * @param parallelizationCount number of pieces to split the workload into
		 * @return this builder
		 */

		public Builder withParallelizationCount(int parallelizationCount) {
			if(parallelizationCount < 1) {
				throw new IllegalArgumentException("Unexpected parallelization count " + parallelizationCount);
			}
			this.parallelizationCount = parallelizationCount;
			return this;
		}

		public Builder withExecutorService(ExecutorService executorService) {
			this.executorService = executorService;

			return this;
		}

		public Builder withAvailableProcessors(int factor) {
			if(factor < 1) {
				throw new IllegalArgumentException("Unexpected available processors factor " + factor);
			}
			this.threads = Math.max(1, Runtime.getRuntime().availableProcessors() / factor);

			return this;
		}
		

		public Builder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			this.pointFilter = pointFilter;
			return this;
		}

		/**
		 * Whether to skip permutations whose reverse has already been searched. A permutation and its reverse produce
		 * equally good complete packings, but possibly different partial packings (a permutation places a prefix of its
		 * boxes), so the skip is only applied when an unplaceable box aborts the attempt, i.e. when packing into a
		 * single container.
		 *
		 * @param filterReversePermutations true to skip reverse-equivalent permutations
		 * @return this builder
		 */
		public Builder withSkipReversePermutations(boolean filterReversePermutations) {
			this.filterReversePermutations = filterReversePermutations;
			return this;
		}

		protected boolean treeSearch = true;
		protected boolean treeSearchUnits = false;
		protected int treeSearchMinBoxes = DEFAULT_TREE_SEARCH_MIN_BOXES;

		/**
		 * Search the box orders of a container as one tree where that gives the result of the loop over the permutations: every prefix of boxes is placed
		 * once and the tree prunes with bounds, see {@link BruteForceTreeSearch}. On by default, for the containers searched on one thread (see
		 * {@link #withTreeSearchUnits(boolean)} for the others).
		 *
		 * @param treeSearch false to search with the loop only
		 * @return this builder
		 */
		public Builder withTreeSearch(boolean treeSearch) {
			this.treeSearch = treeSearch;
			return this;
		}

		/**
		 * Split a container's orders between the threads by their first boxes, each unit searched as a tree, instead of splitting the permutations
		 * of the loop. Off by default: the tree prunes the first boxes it tries last with the arrangements it found under the first ones, which the
		 * units, which search the first boxes side by side, cannot, so they do more work in total; on the benchmarks the permutation units are
		 * quicker. The result is the same either way (among several complete arrangements the threads' timing decides).
		 *
		 * @param treeSearchUnits true to split by the first boxes
		 * @return this builder
		 */
		public Builder withTreeSearchUnits(boolean treeSearchUnits) {
			this.treeSearchUnits = treeSearchUnits;
			return this;
		}

		protected boolean treeSearchStealing = true;

		/**
		 * Search a container's orders as one tree on all the threads: a thread which runs out of work steals the shallowest subtree another thread
		 * has still to search (the types it has still to try at a node near the root), so the tree's order of search, which prunes the first boxes
		 * tried last with what was found under the first ones, is kept, and the threads stay busy. On by default. The result is the same as on one
		 * thread, except that among several complete arrangements the threads' timing decides, and among equal arrangements of the same order the
		 * first found.
		 *
		 * @param treeSearchStealing true to search as one tree with work stealing
		 * @return this builder
		 */
		public Builder withTreeSearchStealing(boolean treeSearchStealing) {
			this.treeSearchStealing = treeSearchStealing;
			return this;
		}

		/**
		 * @param minBoxes the fewest boxes of a container for the tree search (default {@value AbstractBruteForcePackager#DEFAULT_TREE_SEARCH_MIN_BOXES})
		 * @return this builder
		 */
		public Builder withTreeSearchMinBoxes(int minBoxes) {
			this.treeSearchMinBoxes = minBoxes;
			return this;
		}

		protected boolean treeSearchAnyBest = true;

		/**
		 * Let the tree search return any arrangement of the best value (load volume, then weight, then boxes), not the one the loop over the
		 * permutations returns among equally good ones: the search ends at the first arrangement which places all boxes, tries the largest boxes first
		 * even when all boxes fit, and does not expand prefixes which can at most tie the best. On by default; false for the loop's arrangement among
		 * equally good ones (the first order's, its first rotations and positions).
		 *
		 * @param anyBest true for any arrangement of the best value
		 * @return this builder
		 */
		public Builder withTreeSearchAnyBest(boolean anyBest) {
			this.treeSearchAnyBest = anyBest;
			return this;
		}
		
		/**
		 * Build the packager. Without an executor service, one with the configured number of threads (by default the
		 * available processors) is created, and shut down when the packager is closed.
		 *
		 * @return the packager
		 * @throws IllegalStateException if both a thread count and an executor service were set, or if an executor service which
		 *         is not a {@link ThreadPoolExecutor} was set without a parallelization count
		 */
		public ParallelBruteForcePackager build() {
			IntermediatePackagerResultComparator comparator = this.comparator;
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			ExecutorService executorService = this.executorService;
			int parallelizationCount = this.parallelizationCount;
			// an executor service created here is shut down when the packager is closed
			boolean ownExecutorService = executorService == null;
			if(executorService == null) {
				int threads = this.threads;
				if(threads == -1) {
					threads = Runtime.getRuntime().availableProcessors();
				}
				if(threadPriority == NO_THREAD_PRIORITY) {
					executorService = Executors.newFixedThreadPool(threads);
				} else {
					executorService = Executors.newFixedThreadPool(threads, new DefaultThreadFactory(threadPriority));
				}
				if(parallelizationCount == -1) {
					parallelizationCount = 16 * threads;
				}
			} else {
				if(threads != -1) {
					throw new IllegalStateException("Not expecting both thread count and executor service");
				}
				if(parallelizationCount == -1) {
					// auto detect
					if(executorService instanceof ThreadPoolExecutor) {
						ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)executorService;
						long maximumPoolSize = threadPoolExecutor.getMaximumPoolSize();
						if(maximumPoolSize > MAX_BOUNDED_POOL_SIZE) {
							// effectively unbounded, i.e. a cached thread pool
							parallelizationCount = 16 * Runtime.getRuntime().availableProcessors();
						} else {
							parallelizationCount = (int)(16L * maximumPoolSize);
						}
					} else {
						throw new IllegalStateException("Expected a parallelization count for custom executor service");
					}
				}
			}
			
			ParallelBruteForcePackager packager = new ParallelBruteForcePackager(executorService, parallelizationCount, comparator, pointFilter, filterReversePermutations);
			packager.setShutdownExecutorServiceOnClose(ownExecutorService);
			packager.setThreadPriority(threadPriority);
			if(containerPackingStrategyFactory != null) {
				packager.setContainerPackingStrategyFactory(containerPackingStrategyFactory);
			}
			if(boxItemGroupComparator != null) {
				packager.setBoxItemGroupComparator(boxItemGroupComparator);
			}
			packager.setRequireFullSupport(requireFullSupport);
			packager.setTreeSearch(treeSearch);
			packager.setTreeSearchMinBoxes(treeSearchMinBoxes);
			packager.setTreeSearchAnyBest(treeSearchAnyBest);
			packager.treeSearchUnits = treeSearchUnits;
			packager.treeSearchStealing = treeSearchStealing;
			return packager;
		}
	}

	private final int parallelizationCount;
	private final ExecutorService executorService;
	protected final BruteForcePointIteratorFilter pointFilter;
	protected final boolean filterReversePermutations;

	/** Whether to split a container's orders between the threads by their first boxes, each unit a tree search (see {@link Builder#withTreeSearchUnits}) */
	private boolean treeSearchUnits;
	/** Whether to search a container's orders as one tree on all the threads, with work stealing (see {@link Builder#withTreeSearchStealing}) */
	private boolean treeSearchStealing;

	/** Priority for the threads which search, or {@linkplain #NO_THREAD_PRIORITY}. Set by the builder, before the packager is returned. */
	private int threadPriority = NO_THREAD_PRIORITY;

	/** The number of attempts which split the orders of the box item groups between the threads (for tests) */
	final AtomicInteger groupOrderSplits = new AtomicInteger();

	public ParallelBruteForcePackager(ExecutorService executorService, int parallelizationCount,
			IntermediatePackagerResultComparator comparator, BruteForcePointIteratorFilter pointFilter) {
		this(executorService, parallelizationCount, comparator, pointFilter, false);
	}

	public ParallelBruteForcePackager(ExecutorService executorService, int parallelizationCount, 
			IntermediatePackagerResultComparator comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
		super(comparator);

		this.parallelizationCount = parallelizationCount;
		this.executorService = executorService;
		this.pointFilter = pointFilter;
		this.filterReversePermutations = filterReversePermutations;
	}

	/**
	 * @param skip whether to skip reverse permutations for this attempt (see {@link #isReverseSymmetric(PackagerInput)})
	 */
	private BoxItemPermutationRotationIterator filterReversePermutations(BoxItemPermutationRotationIterator iterator, boolean skip) {
		if(!filterReversePermutations || !skip) {
			return iterator;
		}

		// A parallel work unit can start in the filtered half of a reverse pair.
		// Move it to its first canonical permutation before the pack loop processes
		// the iterator's current state.
		while(!FilteredReversedBoxItemPermutationRotationIterator.isCanonical(iterator.getPermutations())) {
			if(iterator.nextPermutation() == -1) {
				return null;
			}
		}
		return new FilteredReversedBoxItemPermutationRotationIterator(iterator);
	}

	/**
	 * Search the orders of a container as one tree on the threads, with work stealing: the first thread searches the whole tree, the others steal
	 * the shallowest subtree a thread has still to search (see {@link BruteForceTreeSearch.StealableState}), and so on until no thread has work.
	 * The threads share their best arrangement for pruning; a complete result stops the others. The results compare by the result comparator, then
	 * by their orders.
	 *
	 * @param iterator the container's iterator, which the threads fork
	 * @param completeBoxCount the number of boxes of a result which no other result can beat
	 * @param truncate the truncation of a result, or null
	 */
	private BruteForceIntermediatePackagerResult packTreeStealing(Workers workers, int containerIndex, ContainerItem containerItem,
			AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts, IntermediatePackagerResult currentBest, PackagerInterruptSupplier sourceInterrupt,
			int completeBoxCount, UnaryOperator<BruteForceIntermediatePackagerResult> truncate) throws PackagerInterruptedException {
		// the parallelization count is 16 per thread by default
		int tasks = Math.max(1, Math.min(workers.size(), parallelizationCount / 16));
		BruteForceTreeSearch.Tables[][] holders = new BruteForceTreeSearch.Tables[tasks][];
		for (int j = 0; j < tasks; j++) {
			holders[j] = workers.get(j).tables;
		}
		return BruteForceTreeStealing.search(executorService, tasks, iterator, insertAfterCounts, sourceInterrupt, completeBoxCount, truncate, intermediatePackagerResultComparator,
				(task, threadIterator, interrupt, work, shared, tables, state) -> {
					BruteForceWorker worker = workers.get(task);
					return packTreeUnit(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, containerIndex, threadIterator, interrupt, pointFilter,
							currentBest, work, shared, tables, state);
				}, holders, this::withThreadPriority);
	}

	/**
	 * Search the orders of a container as a tree on the threads: the orders are split into units by their first types (see
	 * {@link BruteForceTreeSearch#prefixes}), which the threads take in turn, each searching its unit as a tree on its own state (see
	 * {@link #packTreeUnit}). The units' results are compared in the order of the units, the first of equal results winning. A complete result
	 * (which no other result can beat) stops the other units, as for the permutation work units, so among several complete arrangements the
	 * threads' timing decides, as before; otherwise the result is the same as on one thread.
	 *
	 * @param iterator the container's iterator, which the units fork
	 * @param completeBoxCount the number of boxes of a result which no other result can beat
	 * @param shareBest whether the units share their best arrangement for pruning (not with results which are truncated afterwards: the units
	 *        compare by their truncated results)
	 * @param truncate the truncation of a unit's result, or null
	 */
	private BruteForceIntermediatePackagerResult packTreeMultithreaded(Workers workers, int containerIndex, ContainerItem containerItem,
			AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts, IntermediatePackagerResult currentBest, PackagerInterruptSupplier sourceInterrupt,
			int completeBoxCount, boolean shareBest, UnaryOperator<BruteForceIntermediatePackagerResult> truncate) throws PackagerInterruptedException {
		// a quarter of the parallelization count of units, with at least 256 orders each: a unit's setup must not outweigh its search
		long wanted = Math.max(1L, Math.min(parallelizationCount / 4, iterator.countPermutations() / 256));
		List<int[]> prefixes = BruteForceTreeSearch.prefixes(iterator, insertAfterCounts, (int)wanted);
		int units = prefixes.size();
		BruteForceIntermediatePackagerResult[] results = new BruteForceIntermediatePackagerResult[units];
		// the next unit to search
		AtomicInteger next = new AtomicInteger();
		// the first unit with a complete result: no other result can beat it
		AtomicInteger complete = new AtomicInteger(Integer.MAX_VALUE);
		BruteForceTreeSearch.SharedBest shared = shareBest ? new BruteForceTreeSearch.SharedBest() : null;
		LocalInterrupt localInterrupt = new LocalInterrupt();

		// one per attempt, see packMultithreaded
		ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
		int tasks = Math.min(units, workers.size());
		List<Future<Void>> futures = new ArrayList<>(tasks);
		for (int j = 0; j < tasks; j++) {
			BruteForceWorker worker = workers.get(j);
			futures.add(executorCompletionService.submit(withThreadPriority(() -> {
				AbstractBoxItemPermutationRotationIterator unitIterator = iterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator ? defaultIterator.fork()
						: ((DefaultBoxItemGroupPermutationRotationIterator)iterator).fork();
				// the thread's units share their tables
				BruteForceTreeSearch.Tables[] tables = new BruteForceTreeSearch.Tables[1];
				for (int unit = next.getAndIncrement(); unit < units && complete.get() == Integer.MAX_VALUE; unit = next.getAndIncrement()) {
					int u = unit;
					PackagerInterruptSupplier unitInterrupt = () -> localInterrupt.interrupted || complete.get() != Integer.MAX_VALUE || sourceInterrupt.getAsBoolean();
					int[] prefix = prefixes.get(u);
					int[] order = BruteForceTreeSearch.firstOrder(unitIterator, prefix);
					if(unitIterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator) {
						defaultIterator.setPermutation(order);
					} else {
						((DefaultBoxItemGroupPermutationRotationIterator)unitIterator).setPermutation(order);
					}
					try {
						BruteForceIntermediatePackagerResult result = packTreeUnit(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, containerIndex,
								unitIterator, unitInterrupt, pointFilter, currentBest, BruteForceTreeSearch.Work.unit(prefix), shared, tables, null);
						if(truncate != null) {
							result = truncate.apply(result);
						}
						results[u] = result;
						if(result != null && result.getBoxCount() >= completeBoxCount) {
							complete.accumulateAndGet(u, Math::min);
						}
					} catch (PackagerInterruptedException e) {
						if(localInterrupt.interrupted || complete.get() == Integer.MAX_VALUE) {
							throw e;
						}
						// stopped: another unit is complete
					}
				}
				return null;
			})));
		}
		try {
			for (int j = 0; j < futures.size(); j++) {
				try {
					executorCompletionService.take().get();
				} catch (ExecutionException e) {
					localInterrupt.interrupted = true;
					Throwable cause = e.getCause();
					if(cause instanceof PackagerInterruptedException) {
						throw (PackagerInterruptedException)cause;
					}
					throw new PackagerException(cause);
				} catch (InterruptedException e) {
					localInterrupt.interrupted = true;
					throw new PackagerInterruptedException();
				}
			}
		} finally {
			for (Future<Void> future : futures) {
				future.cancel(true);
			}
		}
		if(sourceInterrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		BruteForceIntermediatePackagerResult best = null;
		for (int u = 0; u < units; u++) {
			BruteForceIntermediatePackagerResult result = results[u];
			if(result != null && !result.isEmpty() && (best == null || intermediatePackagerResultComparator.compare(best, result) < 0)) {
				best = result;
			}
		}
		return best;
	}

	/**
	 * A work unit of the search: its state is created on the thread which searches (see {@link #call()}), and only if
	 * the work unit is searched (the other work units are skipped when a search is found to be complete).
	 */
	private class BruteForceWorker implements Callable<BruteForceIntermediatePackagerResult> {

		private ContainerItem containerItem;
		/** The iterators of the work units: each is created when its work unit is searched. */
		private IntFunction<BoxItemPermutationRotationIterator> units;
		private int unit;
		private boolean filterReverse;
		/** Whether the search was found to be complete, i.e. whether the results of the other work units are not needed */
		private LocalInterrupt localInterrupt;
		private Placement[] placements;
		private int placementCount;
		private PointCalculator3DStack pointCalculator;
		private PackagerInterruptSupplier interrupt;
		private int containerIndex;
		private IntermediatePackagerResult best;

		private final int placementCapacity;
		private final int maxIteratorLength;
		/** Whether the boxes have load limits: then the placements track loads */
		private final boolean load;
		/** the tree search's tables of the worker's parts, kept over the searches */
		private final BruteForceTreeSearch.Tables[] tables = new BruteForceTreeSearch.Tables[1];

		public BruteForceWorker(int placementCapacity, int placementCount, int maxIteratorLength, boolean load) {
			this.load = load;
			this.placementCapacity = placementCapacity;
			this.placementCount = placementCount;
			this.maxIteratorLength = maxIteratorLength;
		}

		/** @return the placements, created when first needed */
		public Placement[] placements() {
			Placement[] placements = this.placements;
			if(placements == null) {
				this.placements = placements = getPlacements(placementCapacity, load);
			}
			return placements;
		}

		/** @return the point calculator, created when first needed */
		public PointCalculator3DStack pointCalculator() {
			PointCalculator3DStack pointCalculator = this.pointCalculator;
			if(pointCalculator == null) {
				pointCalculator = new PointCalculator3DStack(maxIteratorLength + 1);
				pointCalculator.reset(1, 1, 1);
				this.pointCalculator = pointCalculator;
			}
			return pointCalculator;
		}

		public void removeFirstPlacements(int size) {
			if(placements != null) {
				placementCount = BruteForcePackager.removeFirstPlacements(placements, size, placementCount);
			} else {
				placementCount -= size;
			}
		}

		public void clearPlacements() {
			placementCount = 0;
		}

		public void setContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
		}
		
		public ContainerItem getContainerItem() {
			return containerItem;
		}

		/**
		 * Search a work unit, whose iterator is created when the search starts.
		 *
		 * @param units the iterators of the work units
		 * @param unit index of the work unit
		 * @param filterReverse whether to skip reverse permutations
		 * @param localInterrupt whether the search is complete
		 */
		public void setWorkUnit(IntFunction<BoxItemPermutationRotationIterator> units, int unit, boolean filterReverse, LocalInterrupt localInterrupt) {
			this.units = units;
			this.unit = unit;
			this.filterReverse = filterReverse;
			this.localInterrupt = localInterrupt;
		}

		public void setInterrupt(PackagerInterruptSupplier interrupt) {
			this.interrupt = interrupt;
		}

		public void setContainerIndex(int containerIndex) {
			this.containerIndex = containerIndex;
		}

		/**
		 * @param best the best result so far, or null: permutations which cannot load more are skipped (see
		 *        {@link AbstractBruteForcePackager#pack(PointCalculator3DStack, Placement[], int, ContainerItem, int, BoxItemPermutationRotationIterator, PackagerInterruptSupplier, BruteForcePointIteratorFilter, IntermediatePackagerResult)})
		 */
		public void setBest(IntermediatePackagerResult best) {
			this.best = best;
		}

		@Override
		public BruteForceIntermediatePackagerResult call() throws PackagerInterruptedException {
			if(interrupt.getAsBoolean()) {
				// interrupted before the work unit started: do not create its state
				if(localInterrupt.interrupted) {
					// the search is complete, the result is not needed
					return null;
				}
				throw new PackagerInterruptedException();
			}
			BoxItemPermutationRotationIterator iterator = filterReversePermutations(units.apply(unit), filterReverse);
			if(iterator == null) {
				return null;
			}
			BruteForceIntermediatePackagerResult result = ParallelBruteForcePackager.this.pack(pointCalculator(), placements(), placementCount, containerItem, containerIndex, iterator, interrupt, pointFilter, best);
			if(result.containsLastBox() || result.getBoxCount() >= localInterrupt.completeBoxCount) {
				// will not match any better than this: stop the work units which did not start yet, and those which did
				localInterrupt.interrupted = true;
			}
			return result;
		}
	}

	/**
	 * The workers of a session, one for each work unit. The workers are created when first needed, so that a search on
	 * a single thread does not create the other workers.
	 */
	private class Workers {

		private final BruteForceWorker[] workers;
		private final int placementCapacity;
		private int placementCount;
		private final int maxIteratorLength;
		private final boolean load;

		private Workers(int count, int placementCount, int maxIteratorLength, boolean load) {
			this.workers = new BruteForceWorker[count];
			this.placementCapacity = placementCount;
			this.placementCount = placementCount;
			this.maxIteratorLength = maxIteratorLength;
			this.load = load;
		}

		/**
		 * @param maxIteratorLength the length of the longest iterator, after boxes were removed
		 * @return workers for another session, which does not share state with this
		 */
		private Workers fork(int maxIteratorLength) {
			return new Workers(workers.length, placementCount, maxIteratorLength, load);
		}

		private int size() {
			return workers.length;
		}

		private BruteForceWorker get(int index) {
			BruteForceWorker worker = workers[index];
			if(worker == null) {
				workers[index] = worker = new BruteForceWorker(placementCapacity, placementCount, maxIteratorLength, load);
			}
			return worker;
		}

		private void removeFirstPlacements(int size) {
			for (BruteForceWorker worker : workers) {
				if(worker != null) {
					worker.removeFirstPlacements(size);
				}
			}
			placementCount -= size;
		}

		private void clearPlacements() {
			for (BruteForceWorker worker : workers) {
				if(worker != null) {
					worker.clearPlacements();
				}
			}
			placementCount = 0;
		}
	}

	private class ParallelSession extends AbstractBruteForceBoxItemSession {

		private Workers workers; // per work unit
		private ParallelBoxItemPermutationRotationIteratorList[] parallelIterators; // per container
		private DefaultBoxItemPermutationRotationIterator[] iterators; // per container
		private final PackagerInterruptSupplier sourceInterrupt;

		protected ParallelSession(List<BoxItem> boxItems, List<ContainerItem> containers, int containerCount,
				Workers workers, DefaultBoxItemPermutationRotationIterator[] iterators,
				ParallelBoxItemPermutationRotationIteratorList[] parallelIterators,
				PackagerInterruptSupplier sourceInterrupt) {
			super(boxItems, containers, containerCount);

			this.workers = workers;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.sourceInterrupt = sourceInterrupt;
		}

		private ParallelSession(ParallelSession source) {
			super(source);
			this.sourceInterrupt = source.sourceInterrupt;
			this.iterators = new DefaultBoxItemPermutationRotationIterator[source.iterators.length];
			this.parallelIterators = new ParallelBoxItemPermutationRotationIteratorList[source.parallelIterators.length];
			int maxIteratorLength = 0;
			for(int i = 0; i < iterators.length; i++) {
				iterators[i] = source.iterators[i].fork();
				parallelIterators[i] = source.parallelIterators[i].fork();
				maxIteratorLength = Math.max(maxIteratorLength, iterators[i].length());
			}
			this.workers = source.workers.fork(maxIteratorLength);
		}

		@Override
		protected ParallelSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemSession(copyInitialBoxItems(), containers, containerCount, sourceInterrupt);
		}

		@Override
		public ParallelSession fork() {
			return new ParallelSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			int threadPriority = ParallelBruteForcePackager.this.threadPriority;
			if(threadPriority == NO_THREAD_PRIORITY) {
				return search(i, currentBest, abortOnAnyBoxTooBig);
			}
			// searching on this thread, too
			int original = applyThreadPriority(threadPriority);
			try {
				return search(i, currentBest, abortOnAnyBoxTooBig);
			} finally {
				Thread.currentThread().setPriority(original);
			}
		}

		private BruteForceIntermediatePackagerResult search(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(isOrdered()) {
				return attemptOrdered(i, currentBest);
			}
			if(iterators[i].length() == 0) {
				// no box fits this container on its own
				return null;
			}
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations
			// the algorithm only splits on permutations
			boolean multithreaded;
			long permutationCount = filterReversePermutations && reverseSymmetric && abortOnAnyBoxTooBig
					? new FilteredReversedBoxItemPermutationRotationIterator(iterators[i]).countPermutations()
					: iterators[i].countPermutations();
			if(permutationCount > 2L * parallelizationCount) {
				multithreaded = true;
			} else {
				multithreaded = false;
			}

			if(multithreaded) {
				boolean filterReverse = reverseSymmetric && abortOnAnyBoxTooBig;
				int[] insertAfterCounts = getInsertAfterCounts(iterators[i]);
				if(treeSearchStealing && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)
						&& !canPackAll(iterators[i], getContainerItem(i).getContainer().getMaxLoadVolume(), getContainerItem(i).getContainer().getMaxLoadWeight())) {
					// when all boxes fit, the bound prunes nothing and the first complete arrangement found wins: the permutation work units find one
					// sooner, or the tree on one thread with any arrangement of the best value (below)
					iterators[i].reset();
					return packTreeStealing(workers, i, getContainerItem(i), iterators[i], insertAfterCounts, currentBest, sourceInterrupt, iterators[i].length(), null);
				}
				if(treeSearchAnyBest && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)) {
					// any arrangement of the best value: the tree tries the largest boxes first and ends at the first complete arrangement, which it
					// finds sooner on one thread than the threads do
					iterators[i].reset();
					BruteForceWorker worker = workers.get(0);
					return ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(i), i, iterators[i], sourceInterrupt,
							pointFilter, currentBest);
				}
				if(treeSearchUnits && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)) {
					iterators[i].reset();
					BruteForceIntermediatePackagerResult result = packTreeMultithreaded(workers, i, getContainerItem(i), iterators[i], insertAfterCounts, currentBest, sourceInterrupt,
							iterators[i].length(), true, null);
					return result;
				}
				// a previous attempt left the work units at their last permutations
				parallelIterators[i].reset();
				LocalInterrupt localInterrupt = new LocalInterrupt();

				// one per attempt: attempts may run concurrently (session forks), and the futures of an attempt are
				// cancelled when it is done, after which they would otherwise be taken by the next attempt
				ExecutorCompletionService<BruteForceIntermediatePackagerResult> executorCompletionService = new ExecutorCompletionService<>(executorService);
				int units = workers.size();
				List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(units);

				ContainerItem containerItem = getContainerItem(i);
				PackagerInterruptSupplier interrupt = () -> localInterrupt.interrupted || sourceInterrupt.getAsBoolean();
				IntFunction<BoxItemPermutationRotationIterator> workUnits = parallelIterators[i]::getIterator;
				for (int j = 0; j < units; j++) {
					if(localInterrupt.interrupted) {
						// the search is complete, so the remaining work units are not needed
						break;
					}
					BruteForceWorker worker = workers.get(j);

					worker.setContainerItem(containerItem);
					worker.setContainerIndex(i);
					worker.setBest(currentBest);
					// the work unit's iterator is created when the work unit starts
					worker.setWorkUnit(workUnits, j, filterReverse, localInterrupt);
					worker.setInterrupt(interrupt);

					futures.add(executorCompletionService.submit(withThreadPriority(worker)));
				}

				try {
					BruteForceIntermediatePackagerResult best = null;
					for (int j = 0; j < futures.size(); j++) {
						try {
							try {
								Future<BruteForceIntermediatePackagerResult> future = executorCompletionService.take();
								BruteForceIntermediatePackagerResult result = future.get();
								if(result != null) {
									if(best == null || intermediatePackagerResultComparator.compare(best, result) < 0) {
										best = result;
										
										if(best.containsLastBox()) { // will not match any better than this
											// cancel others
											localInterrupt.interrupted = true;
											// don't break, so we're waiting for all the remaining threads to finish
										}
									}
								}
							} catch (ExecutionException e1) {
								Throwable cause = e1.getCause();
								if(cause instanceof PackagerInterruptedException) {
									if(localInterrupt.interrupted) {
										continue;
									}
									throw (PackagerInterruptedException)cause;
								}
								throw e1.getCause();
							}
						} catch (InterruptedException e1) {
							// ignore
							localInterrupt.interrupted = true;
							return null;
						} catch (PackagerInterruptedException e) {
							localInterrupt.interrupted = true;
							throw e;
						} catch (Throwable e) {
							localInterrupt.interrupted = true;
							throw new PackagerException(e);
						}
					}
					// was the search interrupted?
					if(sourceInterrupt.getAsBoolean()) {
						throw new PackagerInterruptedException();
					}
					return best;
				} finally {
					for (Future<BruteForceIntermediatePackagerResult> future : futures) {
						future.cancel(true);
					}
				}
			}
			
			ContainerItem containerItem = getContainerItem(i);
			
			// no need to split this job
			// run with linear approach, from the first permutation
			iterators[i].reset();
			BoxItemPermutationRotationIterator iterator = filterReversePermutations(iterators[i], reverseSymmetric && abortOnAnyBoxTooBig);
			BruteForceWorker worker = workers.get(0);
			return ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, i, iterator,
					sourceInterrupt, pointFilter, currentBest);
		}

		/**
		 * With a box item order (one permutation) or container priorities (boxes permuted within blocks of the same
		 * priority), the permutations are not split between the threads: search on this thread, like
		 * {@link BruteForcePackager}.
		 */
		private BruteForceIntermediatePackagerResult attemptOrdered(int i, IntermediatePackagerResult best) throws PackagerInterruptedException {
			DefaultBoxItemPermutationRotationIterator iterator = iterators[i];
			if(iterator.length() == 0) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			iterator.reset();
			BruteForceWorker worker = workers.get(0);
			ContainerItem containerItem = getContainerItem(i);
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
				return packInOrderSkipping(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, i, iterator, sourceInterrupt, pointFilter, null, getMaxContainerPriority(iterator), best);
			}
			if(order != Order.NONE) {
				return packInOrder(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, i, iterator, sourceInterrupt, pointFilter, best, getLimit(iterator));
			}
			return pack(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, i, iterator, sourceInterrupt, pointFilter, best, getLimit(iterator));
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {
				
				bruteForceResult.markDirty();
				Stack stack = bruteForceResult.getStack();
				
				Container container = packagerContainerItems.toContainer(resolveContainerItem(bruteForceResult), stack);
				
				if(!bruteForceResult.containsLastBox()) {
					// this result does not consume all placements
					// remove consumed items from the iterators
	
					int size = container.getStack().size();
	
					PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
	
					int[] permutations = state.getPermutations();
					List<Integer> p = new ArrayList<>(size);
					for (int i = 0; i < size; i++) {
						p.add(permutations[i]);
					}
	
					for (ParallelBoxItemPermutationRotationIteratorList it : parallelIterators) {
						it.removePermutations(p);
					}
	
					for (DefaultBoxItemPermutationRotationIterator it : iterators) {
						it.removePermutations(p);
					}
					
					// remove session inventory
					removeInventory(p);
	
					workers.removeFirstPlacements(size);
				} else {
					workers.clearPlacements();
					for(int i = 0; i < boxesRemaining.length; i++) {
						boxesRemaining[i] = 0;
					}
				}
				return container;
			} else {
				Stack stack = result.getStack();
				Container container = packagerContainerItems.toContainer(resolveContainerItem(result), stack);
				List<Integer> permutations = getLocalIndexes(stack);

				for (ParallelBoxItemPermutationRotationIteratorList iterator : parallelIterators) {
					iterator.removePermutations(permutations);
				}
				for (DefaultBoxItemPermutationRotationIterator iterator : iterators) {
					iterator.removePermutations(permutations);
				}
				removeInventory(permutations);
				workers.removeFirstPlacements(permutations.size());
				return container;
			}
		}

		@Override
		public int countRemainingBoxes() {
			int count = 0;
			for (int i : boxesRemaining) {
				count += i;
			}
			return count;
		}

	}

	private class ParallelGroupSession extends AbstractBruteForceBoxItemGroupSession {

		/** the tree search's tables of the searches on the calling thread, reused over the group orders */
		private final BruteForceTreeSearch.Tables[] tables = new BruteForceTreeSearch.Tables[1];

		private Workers workers; // per work unit
		private ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators; // per container
		private DefaultBoxItemGroupPermutationRotationIterator[] iterators; // per container
		private final PackagerInterruptSupplier sourceInterrupt;

		protected ParallelGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, 
				List<ContainerItem> containers, int containerCount, Workers workers,
				DefaultBoxItemGroupPermutationRotationIterator[] iterators,
				ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators,
				PackagerInterruptSupplier sourceInterrupt) {
			super(boxItems, containers, containerCount, boxItemGroups);
			this.workers = workers;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.sourceInterrupt = sourceInterrupt;
		}

		private ParallelGroupSession(ParallelGroupSession source) {
			super(source);
			this.sourceInterrupt = source.sourceInterrupt;
			this.iterators = new DefaultBoxItemGroupPermutationRotationIterator[source.iterators.length];
			this.parallelIterators = new ParallelBoxItemGroupPermutationRotationIteratorList[source.parallelIterators.length];
			int maxIteratorLength = 0;
			for(int i = 0; i < iterators.length; i++) {
				iterators[i] = source.iterators[i].fork();
				parallelIterators[i] = source.parallelIterators[i].fork();
				maxIteratorLength = Math.max(maxIteratorLength, iterators[i].length());
			}
			this.workers = source.workers.fork(maxIteratorLength);
		}

		@Override
		protected ParallelGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), containers, containerCount, sourceInterrupt);
		}

		@Override
		public ParallelGroupSession fork() {
			return new ParallelGroupSession(this);
		}

		/**
		 * Groups are packed in order, so a container holds the groups up to the first which it excludes (because it does
		 * not fit): a result with all their boxes is the best there is, as for a container which excludes no group a
		 * result with all the boxes is (see {@link #truncateToWholeGroups} and {@link #truncateToGroup}, which keep
		 * the same groups). A result of the iterator which holds this many boxes holds these groups, in the first
		 * permutation positions.
		 *
		 * @param iteratorGroups the groups of the work units' iterator, in their order (null if excluded)
		 * @param wholeGroups see {@link #packMultithreaded}
		 * @return the number of boxes of the groups which the container can hold, or {@linkplain Integer#MAX_VALUE} if none
		 */
		private int getCompleteBoxCount(BoxItemGroup[] iteratorGroups, boolean wholeGroups) {
			int count = 0;
			if(wholeGroups) {
				for (BoxItemGroup group : iteratorGroups) {
					if(group == null) {
						break;
					}
					count += group.getBoxCount();
				}
			} else {
				for (int k = 0; k < boxItemGroups.size(); k++) {
					if(iteratorGroups[remainingGroupPositions.get(k)] == null) {
						break;
					}
					count += boxItemGroups.get(k).getBoxCount();
				}
			}
			return count > 0 ? count : Integer.MAX_VALUE;
		}

		/**
		 * Search a container's permutations on the threads, split into work units.
		 *
		 * @param units the work units
		 * @param iteratorGroups the groups of the work units' iterator, in their order (null if excluded)
		 * @param wholeGroups true to keep the whole groups of results (see {@link #truncateToWholeGroups}), false to
		 *        keep the whole remaining groups (see {@link #truncateToGroup})
		 * @param filterReverse whether to skip reverse permutations
		 * @return the best result (holding whole groups), or null
		 */
		private BruteForceIntermediatePackagerResult packMultithreaded(int i, ParallelBoxItemGroupPermutationRotationIteratorList units, BoxItemGroup[] iteratorGroups, boolean wholeGroups,
				IntermediatePackagerResult currentBest, boolean filterReverse) throws PackagerInterruptedException {
			LocalInterrupt localInterrupt = new LocalInterrupt(getCompleteBoxCount(iteratorGroups, wholeGroups));

			// one per attempt: attempts may run concurrently (session forks), and the futures of an attempt are
			// cancelled when it is done, after which they would otherwise be taken by the next attempt
			ExecutorCompletionService<BruteForceIntermediatePackagerResult> executorCompletionService = new ExecutorCompletionService<>(executorService);
			List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(workers.size());
			ContainerItem containerItem = getContainerItem(i);
			PackagerInterruptSupplier interrupt = () -> localInterrupt.interrupted || sourceInterrupt.getAsBoolean();
			IntFunction<BoxItemPermutationRotationIterator> workUnits = units::getIterator;
			for (int j = 0; j < workers.size(); j++) {
				if(localInterrupt.interrupted) {
					// the search is complete, so the remaining work units are not needed
					break;
				}
				BruteForceWorker worker = workers.get(j);

				worker.setContainerItem(containerItem);
				worker.setContainerIndex(i);
				worker.setBest(currentBest);
				// the work unit's iterator is created when the work unit starts
				worker.setWorkUnit(workUnits, j, filterReverse, localInterrupt);
				worker.setInterrupt(interrupt);

				futures.add(executorCompletionService.submit(withThreadPriority(worker)));
			}

			try {
				BruteForceIntermediatePackagerResult best = null;
				for (int j = 0; j < futures.size(); j++) {
					try {
						try {
							Future<BruteForceIntermediatePackagerResult> future = executorCompletionService.take();
							
							BruteForceIntermediatePackagerResult result = wholeGroups ? truncateToWholeGroups(future.get(), iteratorGroups) : truncateToGroup(future.get(), iteratorGroups);
							if(result != null) {
								if(best == null || intermediatePackagerResultComparator.compare(best, result) < 0) {
									best = result;
									
									if(best.containsLastBox()) { // will not match any better than this
										// cancel others
										localInterrupt.interrupted = true;
										// don't break, so we're waiting for all the remaining threads to finish
									}
								}
							}
						} catch (ExecutionException e1) {
							Throwable cause = e1.getCause();
							if(cause instanceof PackagerInterruptedException) {
								if(localInterrupt.interrupted) {
									continue;
								}
								throw (PackagerInterruptedException)cause;
							}
							throw e1.getCause();
						}
					} catch (InterruptedException e1) {
						// ignore
						localInterrupt.interrupted = true;
						return null;
					} catch (PackagerInterruptedException e) {
						localInterrupt.interrupted = true;
						throw e;
					} catch (Throwable e) {
						localInterrupt.interrupted = true;
						throw new PackagerException(e);
					}
				}
				// was the search interrupted?
				if(sourceInterrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				return best;
			} finally {
				for (Future<BruteForceIntermediatePackagerResult> future : futures) {
					future.cancel(true);
				}
			}
		}

		/**
		 * Search the orders of the groups on the threads. With few orders of groups with many permutations, the orders are
		 * searched one at a time, and the permutations of each are split between the threads (see
		 * {@link #packGroupOrder}). Otherwise the orders are split into work units by their first groups (prefixes),
		 * which the threads take in turn; each unit is searched
		 * like {@link #attemptGroupOrders} searches all orders on one thread (see {@link #searchGroupOrders}). The
		 * threads share the best result so far, for pruning, and the units after a unit with a result which holds all
		 * groups stop. The units' results are compared in the order of the units, the first of equal results winning, so
		 * the result is the same as on one thread.
		 */
		@Override
		protected BruteForceIntermediatePackagerResult attemptGroupOrders(int containerIndex, IntermediatePackagerResult best) throws PackagerInterruptedException {
			if(isFewGroupOrders(boxItemGroups.size()) && iterators[containerIndex].countPermutations() > 2L * parallelizationCount) {
				return super.attemptGroupOrders(containerIndex, best);
			}
			groupOrderSplits.incrementAndGet();
			int prefixLength = getGroupOrderPrefixLength(boxItemGroups.size());
			return searchGroupOrdersMultithreaded(containerIndex, getGroupOrderPrefixes(prefixLength), prefixLength, best);
		}

		/**
		 * @return true if the orders of the groups are too few to split into enough units for the threads to share (a
		 *         quarter of the parallelization count)
		 */
		private boolean isFewGroupOrders(int groupCount) {
			long orders = 1;
			for (int k = 2; k <= groupCount; k++) {
				orders *= k;
				if(orders * 4 >= parallelizationCount) {
					return false;
				}
			}
			return true;
		}

		/**
		 * @return the shortest prefix length which splits the orders into enough units for the threads to share (a
		 *         quarter of the parallelization count), at most one less than the number of groups
		 */
		private int getGroupOrderPrefixLength(int groupCount) {
			long units = 1;
			for (int k = 1; k < groupCount; k++) {
				units *= groupCount - k + 1;
				if(units * 4 >= parallelizationCount) {
					return k;
				}
			}
			return groupCount - 1;
		}

		/**
		 * @return the prefixes of the orders, lexicographically; without the prefixes of orders which only break the
		 *         container priorities
		 */
		private List<int[]> getGroupOrderPrefixes(int prefixLength) {
			List<int[]> prefixes = new ArrayList<>();
			int[] groupOrder = getFirstGroupOrder(new int[0]);
			do {
				// the first order with the prefix: the other groups in ascending order, so in ascending container priority
				if(getContainerPriorityViolation(groupOrder) == -1) {
					prefixes.add(Arrays.copyOf(groupOrder, prefixLength));
				}
			} while(nextGroupOrder(groupOrder, prefixLength - 1));
			return prefixes;
		}

		private BruteForceIntermediatePackagerResult searchGroupOrdersMultithreaded(int containerIndex, List<int[]> prefixes, int prefixLength, IntermediatePackagerResult best)
				throws PackagerInterruptedException {
			int units = prefixes.size();
			BruteForceIntermediatePackagerResult[] results = new BruteForceIntermediatePackagerResult[units];
			// the next unit to search
			AtomicInteger next = new AtomicInteger();
			// the first unit with a result which holds all groups: the units after it cannot give a better result
			AtomicInteger complete = new AtomicInteger(Integer.MAX_VALUE);
			// the result with the most load volume so far, for pruning
			AtomicReference<IntermediatePackagerResult> shared = new AtomicReference<>();
			int boxCount = 0;
			for (BoxItemGroup group : boxItemGroups) {
				boxCount += group.getBoxCount();
			}
			int allBoxes = boxCount;
			ContainerItem containerItem = getContainerItem(containerIndex);
			LocalInterrupt localInterrupt = new LocalInterrupt();

			// one per attempt, see packMultithreaded
			ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
			int tasks = Math.min(units, workers.size());
			List<Future<Void>> futures = new ArrayList<>(tasks);
			for (int j = 0; j < tasks; j++) {
				BruteForceWorker worker = workers.get(j);
				PackagerInterruptSupplier interrupt = sourceInterrupt;
				futures.add(executorCompletionService.submit(withThreadPriority(() -> {
					for (int unit = next.getAndIncrement(); unit < units && unit < complete.get(); unit = next.getAndIncrement()) {
						int u = unit;
						PackagerInterruptSupplier unitInterrupt = () -> localInterrupt.interrupted || complete.get() < u || interrupt.getAsBoolean();
						try {
							BruteForceIntermediatePackagerResult result = searchGroupOrders(containerIndex, getFirstGroupOrder(prefixes.get(u)), prefixLength, best, shared,
									(index, iterator, groupOrder, hint) -> {
										if(unitInterrupt.getAsBoolean()) {
											throw new PackagerInterruptedException();
										}
										return ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, containerItem, index, iterator,
												unitInterrupt, pointFilter, hint);
									});
							results[u] = result;
							if(result != null && result.getBoxCount() == allBoxes) {
								complete.accumulateAndGet(u, Math::min);
							}
						} catch (PackagerInterruptedException e) {
							if(localInterrupt.interrupted || complete.get() >= u) {
								throw e;
							}
							// stopped: a unit before this one holds all groups
						}
					}
					return null;
				})));
			}
			try {
				for (int j = 0; j < futures.size(); j++) {
					try {
						executorCompletionService.take().get();
					} catch (ExecutionException e) {
						localInterrupt.interrupted = true;
						Throwable cause = e.getCause();
						if(cause instanceof PackagerInterruptedException) {
							throw (PackagerInterruptedException)cause;
						}
						throw new PackagerException(cause);
					} catch (InterruptedException e) {
						localInterrupt.interrupted = true;
						throw new PackagerInterruptedException();
					}
				}
			} finally {
				for (Future<Void> future : futures) {
					future.cancel(true);
				}
			}
			// was the search interrupted?
			if(sourceInterrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// the first of the best results, in the order of the units (as on one thread)
			BruteForceIntermediatePackagerResult bestResult = null;
			for (BruteForceIntermediatePackagerResult result : results) {
				if(result != null && (bestResult == null || intermediatePackagerResultComparator.compare(bestResult, result) < 0)) {
					bestResult = result;
				}
			}
			return bestResult;
		}

		@Override
		protected BruteForceIntermediatePackagerResult packGroupOrder(int containerIndex, DefaultBoxItemGroupPermutationRotationIterator iterator, int[] groupOrder,
				IntermediatePackagerResult best) throws PackagerInterruptedException {
			if(iterator.countPermutations() > 2L * parallelizationCount) {
				int[] insertAfterCounts = getInsertAfterCounts(iterator);
				if(treeSearchStealing && isTreeSearchable(getContainerItem(containerIndex), iterator, insertAfterCounts)
						&& !canPackAll(iterator, getContainerItem(containerIndex).getContainer().getMaxLoadVolume(), getContainerItem(containerIndex).getContainer().getMaxLoadWeight())) {
					BoxItemGroup[] iteratorGroups = iterator.getBoxItemGroups();
					iterator.reset();
					return packTreeStealing(workers, containerIndex, getContainerItem(containerIndex), iterator, insertAfterCounts, best, sourceInterrupt,
							getCompleteBoxCount(iteratorGroups, true), result -> truncateToWholeGroups(result, iteratorGroups));
				}
				if(treeSearchAnyBest && isTreeSearchable(getContainerItem(containerIndex), iterator, insertAfterCounts)) {
					BruteForceWorker worker = workers.get(0);
					return ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(containerIndex), containerIndex, iterator,
							sourceInterrupt, pointFilter, best, Integer.MAX_VALUE, tables);
				}
				if(treeSearchUnits && isTreeSearchable(getContainerItem(containerIndex), iterator, insertAfterCounts)) {
					BoxItemGroup[] iteratorGroups = iterator.getBoxItemGroups();
					iterator.reset();
					return packTreeMultithreaded(workers, containerIndex, getContainerItem(containerIndex), iterator, insertAfterCounts, best, sourceInterrupt,
							getCompleteBoxCount(iteratorGroups, true), false, result -> truncateToWholeGroups(result, iteratorGroups));
				}
				// split the order's permutations between the threads, which share the iterator's rotations
				ParallelBoxItemGroupPermutationRotationIteratorList units = ParallelBoxItemGroupPermutationRotationIteratorList.of(iterator, parallelizationCount);
				return packMultithreaded(containerIndex, units, units.getBoxItemGroups(), true, best, false);
			}
			// few permutations: search on this thread
			BruteForceWorker worker = workers.get(0);
			return ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(containerIndex), containerIndex, iterator, sourceInterrupt, pointFilter, best);
		}

		@Override
		protected IntermediatePackagerResultComparator getIntermediatePackagerResultComparator() {
			return intermediatePackagerResultComparator;
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			int threadPriority = ParallelBruteForcePackager.this.threadPriority;
			if(threadPriority == NO_THREAD_PRIORITY) {
				return search(i, currentBest, abortOnAnyBoxTooBig);
			}
			// searching on this thread, too
			int original = applyThreadPriority(threadPriority);
			try {
				return search(i, currentBest, abortOnAnyBoxTooBig);
			} finally {
				Thread.currentThread().setPriority(original);
			}
		}

		private BruteForceIntermediatePackagerResult search(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(isGroupOrderSearch()) {
				return attemptGroupOrders(i, currentBest);
			}
			BoxItemGroup[] iteratorGroups = iterators[i].getBoxItemGroups();
			// when skipping, the first group may be skipped
			if(order != Order.CHRONOLOGICAL_ALLOW_SKIPPING && !canLoadNextGroup(iteratorGroups)) {
				return null;
			}
			if(order != Order.NONE) {
				// a box item order: one permutation, searched on this thread
				iterators[i].reset();
				BruteForceWorker worker = workers.get(0);
				if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
					// groups are skipped whole
					return packInOrderSkipping(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(i), i, iterators[i], sourceInterrupt, pointFilter,
							getGroupSkipEnds(iteratorGroups, iterators[i].length()), getMaxContainerPriority(iterators[i]), currentBest);
				}
				return truncateToGroup(packInOrder(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(i), i, iterators[i], sourceInterrupt, pointFilter, currentBest, Integer.MAX_VALUE), iteratorGroups);
			}
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations
			// the algorithm only splits on permutations
			boolean multithreaded;
			long permutationCount = filterReversePermutations && reverseSymmetric && abortOnAnyBoxTooBig
					? new FilteredReversedBoxItemPermutationRotationIterator(iterators[i]).countPermutations()
					: iterators[i].countPermutations();
			if(permutationCount > 2L * parallelizationCount) {
				multithreaded = true;
			} else {
				multithreaded = false;
			}
			
			if(multithreaded) {
				boolean filterReverse = reverseSymmetric && abortOnAnyBoxTooBig;
				int[] insertAfterCounts = getInsertAfterCounts(iterators[i]);
				if(treeSearchStealing && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)
						&& !canPackAll(iterators[i], getContainerItem(i).getContainer().getMaxLoadVolume(), getContainerItem(i).getContainer().getMaxLoadWeight())) {
					iterators[i].reset();
					return packTreeStealing(workers, i, getContainerItem(i), iterators[i], insertAfterCounts, currentBest, sourceInterrupt, getCompleteBoxCount(iteratorGroups, false),
							result -> truncateToGroup(result, iteratorGroups));
				}
				if(treeSearchAnyBest && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)) {
					iterators[i].reset();
					BruteForceWorker worker = workers.get(0);
					return truncateToGroup(ParallelBruteForcePackager.this.pack(worker.pointCalculator(), worker.placements(), worker.placementCount, getContainerItem(i), i, iterators[i],
							sourceInterrupt, pointFilter, currentBest), iteratorGroups);
				}
				if(treeSearchUnits && !(filterReversePermutations && filterReverse) && isTreeSearchable(getContainerItem(i), iterators[i], insertAfterCounts)) {
					iterators[i].reset();
					return packTreeMultithreaded(workers, i, getContainerItem(i), iterators[i], insertAfterCounts, currentBest, sourceInterrupt, getCompleteBoxCount(iteratorGroups, false),
							false, result -> truncateToGroup(result, iteratorGroups));
				}
				// a previous attempt left the work units at their last permutations
				parallelIterators[i].reset();
				return packMultithreaded(i, parallelIterators[i], iteratorGroups, false, currentBest, reverseSymmetric && abortOnAnyBoxTooBig);
			}
			
			ContainerItem containerItem = getContainerItem(i);
			
			// no need to split this job
			// run with linear approach, from the first permutation
			iterators[i].reset();
			BoxItemPermutationRotationIterator iterator = filterReversePermutations(iterators[i], reverseSymmetric && abortOnAnyBoxTooBig);
			BruteForceWorker worker = workers.get(0);
			return truncateToGroup(ParallelBruteForcePackager.this.pack(
					worker.pointCalculator(),
					worker.placements(),
					worker.placementCount,
					containerItem,
					i,
					iterator,
					sourceInterrupt,
					pointFilter,
					currentBest
			), iteratorGroups);
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			// results for another order of the groups (see attemptGroupOrders) hold any of the remaining groups
			if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult && !bruteForceResult.isAnyRemaining()) {
				
				bruteForceResult.markDirty();
				Stack stack = bruteForceResult.getStack();
				
				Container container = packagerContainerItems.toContainer(resolveContainerItem(bruteForceResult), stack);
	
				if(!bruteForceResult.containsLastBox()) {
					// this result does not consume all placements
					// remove consumed items from the iterators
	
					int size = container.getStack().size();
	
					PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
	
					// this result does not consume all placements
					// remove consumed items from the iterators
	
					// results from this session hold the first remaining groups
					List<Integer> removedGroups = new ArrayList<>();
					int wholeGroupBoxCount = 0;
					for(int i = 0; i < boxItemGroups.size(); i++) {
						BoxItemGroup boxItemGroup = boxItemGroups.get(i);
						
						int groupBoxCount = boxItemGroup.getBoxCount();
						if(size < wholeGroupBoxCount + groupBoxCount) {
							// the last group was not successful
							throw new IllegalStateException("Expected to consume whole groups, but group " + i + " was not fully consumed");
						}
						
						removedGroups.add(i);
						
						wholeGroupBoxCount += groupBoxCount;
						
						if(wholeGroupBoxCount == size) {
							break;
						}
					}
					
					int[] permutations = state.getPermutations();
					
					List<Integer> p = new ArrayList<>();
					for(Integer removedGroup: removedGroups) {
						BoxItemGroup boxItemGroup = boxItemGroups.get(removedGroup);
	
						for (BoxItem boxItem : boxItemGroup.getItems()) {
							for (int i = 0; i < boxItem.getCount(); i++) {
								p.add(permutations[p.size()]);
							}
						}
					}						
	
					// remove stacked items which did not make it
					stack.setSize(p.size());
	
					List<Integer> iteratorGroupIndexes = acceptGroups(removedGroups);
					for (ParallelBoxItemGroupPermutationRotationIteratorList it : parallelIterators) {
						it.removeGroups(iteratorGroupIndexes);
					}
	
					for (DefaultBoxItemGroupPermutationRotationIterator it : iterators) {
						it.removeGroups(iteratorGroupIndexes);
					}
					
					// remove session inventory
					removeInventory(p);
	
					workers.removeFirstPlacements(p.size());
				} else {
					workers.clearPlacements();
					for(int i = 0; i < boxesRemaining.length; i++) {
						boxesRemaining[i] = 0;
					}
				}
				return container;
			} else {
				refreshStack(result);
				Stack stack = result.getStack();
				AcceptedGroups accepted = getAcceptedGroups(stack);
				Container container = packagerContainerItems.toContainer(resolveContainerItem(result), stack);

				List<Integer> iteratorGroupIndexes = acceptGroups(accepted.groupIndexes());
				for (ParallelBoxItemGroupPermutationRotationIteratorList iterator : parallelIterators) {
					iterator.removeGroups(iteratorGroupIndexes);
				}
				for (DefaultBoxItemGroupPermutationRotationIterator iterator : iterators) {
					iterator.removeGroups(iteratorGroupIndexes);
				}
				removeInventory(accepted.localIndexes());
				workers.removeFirstPlacements(accepted.localIndexes().size());
				return container;
			}
		}

		@Override
		public int countRemainingBoxes() {
			int count = 0;
			for (int i : boxesRemaining) {
				count += i;
			}
			return count;
		}
	}

	/** Whether the builder created the executor service: then it is shut down when the packager is closed */
	private boolean shutdownExecutorServiceOnClose;

	protected void setShutdownExecutorServiceOnClose(boolean shutdownExecutorServiceOnClose) {
		this.shutdownExecutorServiceOnClose = shutdownExecutorServiceOnClose;
	}

	protected void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	/**
	 * @return the thread priority for the threads which search, or -1 for none
	 */
	int getThreadPriority() {
		return threadPriority;
	}

	/**
	 * Set the priority of the current thread.
	 *
	 * @param threadPriority the new priority
	 * @return the priority which the thread had, for restoring it when done
	 */
	private static int applyThreadPriority(int threadPriority) {
		Thread thread = Thread.currentThread();
		int original = thread.getPriority();
		thread.setPriority(threadPriority);
		return original;
	}

	/**
	 * @param task a task for the executor service
	 * @return the task itself if there is no thread priority, otherwise one which runs the task at the thread priority
	 */
	private <T> Callable<T> withThreadPriority(Callable<T> task) {
		if(threadPriority == NO_THREAD_PRIORITY) {
			return task;
		}
		return new PrioritizedCallable<>(task, threadPriority);
	}

	/** Runs a task with the priority of the current thread set, restoring the priority afterwards */
	private static final class PrioritizedCallable<T> implements Callable<T> {

		private final Callable<T> delegate;
		private final int threadPriority;

		private PrioritizedCallable(Callable<T> delegate, int threadPriority) {
			this.delegate = delegate;
			this.threadPriority = threadPriority;
		}

		@Override
		public T call() throws Exception {
			int original = applyThreadPriority(threadPriority);
			try {
				return delegate.call();
			} finally {
				Thread.currentThread().setPriority(original);
			}
		}
	}

	@Override
	public void close() {
		super.close();
		if(shutdownExecutorServiceOnClose) {
			executorService.shutdownNow();
		}
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	int getParallelizationCount() {
		return parallelizationCount;
	}

	@Override
	protected ParallelSession createBoxItemSession(List<BoxItem> items, List<ContainerItem> containerItems,
			int containerCount, PackagerInterruptSupplier interrupt) {
		
		ParallelBoxItemPermutationRotationIteratorList[] parallelIterators = new ParallelBoxItemPermutationRotationIteratorList[containerItems.size()];
		DefaultBoxItemPermutationRotationIterator[] iterators = new DefaultBoxItemPermutationRotationIterator[containerItems.size()];
		for (int i = 0; i < containerItems.size(); i++) {
			Container container = containerItems.get(i).getContainer();

			iterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(items)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();

			// the work units share the rotations of the iterator
			parallelIterators[i] = ParallelBoxItemPermutationRotationIteratorList.of(iterators[i], parallelizationCount);
		}

		int maxIteratorLength = 0;
		for (DefaultBoxItemPermutationRotationIterator iterator : iterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		int count = 0;
		for (BoxItem stackable : items) {
			count += stackable.getCount();
		}

		boolean load = hasLoadLimits(items);

		Workers workers = new Workers(parallelizationCount, count, maxIteratorLength, load);

		return new ParallelSession(items, containerItems, containerCount, workers, iterators, parallelIterators, interrupt);
	}

	@Override
	protected ParallelGroupSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups,
			List<ContainerItem> containerItems, int containerCount, PackagerInterruptSupplier interrupt) {
		
		ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators = new ParallelBoxItemGroupPermutationRotationIteratorList[containerItems.size()];
		DefaultBoxItemGroupPermutationRotationIterator[] iterators = new DefaultBoxItemGroupPermutationRotationIterator[containerItems.size()];
		for (int i = 0; i < containerItems.size(); i++) {
			Container container = containerItems.get(i).getContainer();

			iterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();

			// the work units share the rotations of the iterator
			parallelIterators[i] = ParallelBoxItemGroupPermutationRotationIteratorList.of(iterators[i], parallelizationCount);
		}

		int maxIteratorLength = 0;
		for (DefaultBoxItemGroupPermutationRotationIterator iterator : iterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		List<BoxItem> items = new ArrayList<BoxItem>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			items.addAll(boxItemGroup.getItems());
		}
		
		int count = 0;
		for (BoxItem boxItem : items) {
			count += boxItem.getCount();
		}

		boolean load = hasLoadLimits(items);

		Workers workers = new Workers(parallelizationCount, count, maxIteratorLength, load);

		return new ParallelGroupSession(items, itemGroups, containerItems, containerCount, workers, iterators, parallelIterators, interrupt);
	}


}
