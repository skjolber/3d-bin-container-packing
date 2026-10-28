package com.github.skjolber.packing.packer.bruteforce.flat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

/**
 * A {@linkplain BruteForceAccelerator} which searches the permutations on threads of the CPU, on the flat model (see {@linkplain FlatSubtree}): the
 * permutations are split in items by the first boxes, the items are searched in parallel (a few nodes at a time, so that the interrupt can be polled
 * between the rounds), and the best arrangements of the items are reduced in the order of the items.
 * <p>
 * It is the reference for devices which run the same flat search (the work of an item is the same), and not a replacement for the packager's own loop
 * on one thread: the flat search is not faster per node, so speed-up comes from the threads only.
 * <p>
 * The interrupt is polled between the items of a round; a single permutation is never interrupted, so how long it takes to react depends on the cost of
 * a permutation.
 * <p>
 * When a search needs more free points per step than {@linkplain Builder#withMaxPoints(int)} allows, the engine gives up, and the packager searches the
 * permutations itself.
 */
public final class FlatEngine implements BruteForceAccelerator {

	/** the default number of free points which the search keeps at once */
	public static final int DEFAULT_MAX_POINTS = 128;
	public static final int DEFAULT_NODE_BUDGET = 20_000;
	public static final int DEFAULT_ITEMS_PER_THREAD = 32;
	public static final int DEFAULT_MAX_ITEMS = 1 << 16;
	public static final int DEFAULT_MIN_BOXES = 8;
	public static final int DEFAULT_MAX_BOXES = 64;

	private static final long POLL_MILLIS = 10L;

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private int threads = Runtime.getRuntime().availableProcessors();
		private int maxPoints = DEFAULT_MAX_POINTS;
		private int nodeBudget = DEFAULT_NODE_BUDGET;
		private int itemsPerThread = DEFAULT_ITEMS_PER_THREAD;
		private int maxItems = DEFAULT_MAX_ITEMS;
		private int minBoxes = DEFAULT_MIN_BOXES;
		private int maxBoxes = DEFAULT_MAX_BOXES;
		private boolean jointTree = false;

		/**
		 * @param threads the number of threads; with one, the search runs on the thread which calls it
		 * @return this builder
		 */
		public Builder withThreads(int threads) {
			if(threads < 1) {
				throw new IllegalArgumentException("Expected at least one thread, got " + threads);
			}
			this.threads = threads;
			return this;
		}

		/**
		 * @param maxPoints the most free points which a search keeps at once; the engine gives up on a problem which needs more
		 * @return this builder
		 */
		public Builder withMaxPoints(int maxPoints) {
			if(maxPoints < 1) {
				throw new IllegalArgumentException("Expected at least one point, got " + maxPoints);
			}
			this.maxPoints = maxPoints;
			return this;
		}

		/**
		 * @param nodeBudget the number of search nodes after which an item pauses, so that the others get their turn and the interrupt is polled
		 * @return this builder
		 */
		public Builder withNodeBudget(int nodeBudget) {
			if(nodeBudget < 1) {
				throw new IllegalArgumentException("Expected a positive node budget, got " + nodeBudget);
			}
			this.nodeBudget = nodeBudget;
			return this;
		}

		/**
		 * @param itemsPerThread the least number of items for each thread, so that the work is spread evenly
		 * @return this builder
		 */
		public Builder withItemsPerThread(int itemsPerThread) {
			if(itemsPerThread < 1) {
				throw new IllegalArgumentException("Expected at least one item per thread, got " + itemsPerThread);
			}
			this.itemsPerThread = itemsPerThread;
			return this;
		}

		/**
		 * @param maxItems the most items: a problem with more item prefixes than this is not searched
		 * @return this builder
		 */
		public Builder withMaxItems(int maxItems) {
			if(maxItems < 1) {
				throw new IllegalArgumentException("Expected at least one item, got " + maxItems);
			}
			this.maxItems = maxItems;
			return this;
		}

		/**
		 * @param minBoxes the fewest boxes of a problem worth searching with the engine: with fewer, the packager's own loop is quicker
		 * @return this builder
		 */
		public Builder withMinBoxes(int minBoxes) {
			if(minBoxes < 0) {
				throw new IllegalArgumentException("Expected zero or more boxes, got " + minBoxes);
			}
			this.minBoxes = minBoxes;
			return this;
		}

		/**
		 * @param maxBoxes the most boxes of a problem which the engine searches
		 * @return this builder
		 */
		public Builder withMaxBoxes(int maxBoxes) {
			if(maxBoxes < 1 || maxBoxes > FlatLayout.MAX_PLACEMENTS) {
				throw new IllegalArgumentException("Expected 1 to " + FlatLayout.MAX_PLACEMENTS + " boxes, got " + maxBoxes);
			}
			this.maxBoxes = maxBoxes;
			return this;
		}

		/**
		 * @param jointTree true to search the orders of an item as one tree ({@linkplain FlatTreeItem}: every prefix placed once), false for the loop over
		 *        the permutations ({@linkplain FlatSubtree}); the winner is the same
		 * @return this builder
		 */
		public Builder withJointTree(boolean jointTree) {
			this.jointTree = jointTree;
			return this;
		}

		public FlatEngine build() {
			return new FlatEngine(threads, maxPoints, nodeBudget, itemsPerThread, maxItems, minBoxes, maxBoxes, jointTree);
		}
	}

	/** The state of one search, shared by its threads. */
	private static final class Run {

		final int n;
		final int[] problem;
		final long[] problemLongs;
		final FlatLayout layout;
		final int depth;
		final int[] prefixes;
		final int items;
		final int outInts;
		final FlatItems flat;
		final int[] out;
		final long[] outLongs;
		final AtomicLong bound;
		/** the first item which places all boxes, or the number of items */
		final AtomicInteger fullItem;
		volatile boolean overflow;
		volatile boolean abort;

		int[] pending;
		int pendingCount;
		AtomicInteger next;

		Run(int n, int[] problem, long[] problemLongs, FlatLayout layout, int depth, int[] prefixes, long boundVolume, int outInts) {
			this.n = n;
			this.problem = problem;
			this.problemLongs = problemLongs;
			this.layout = layout;
			this.depth = depth;
			this.prefixes = prefixes;
			this.flat = new FlatItems(n, prefixes.length / n, outInts);
			this.items = flat.items();
			this.outInts = flat.outInts();
			this.out = flat.out();
			this.outLongs = flat.outLongs();
			this.bound = new AtomicLong(boundVolume);
			this.fullItem = new AtomicInteger(items);
			this.pending = new int[items];
		}
	}

	private final int threads;
	private final int maxPoints;
	private final int nodeBudget;
	private final int itemsPerThread;
	private final int maxItems;
	private final int minBoxes;
	private final int maxBoxes;
	private final boolean jointTree;

	private ExecutorService executor;
	private boolean closed;

	private FlatEngine(int threads, int maxPoints, int nodeBudget, int itemsPerThread, int maxItems, int minBoxes, int maxBoxes, boolean jointTree) {
		this.threads = threads;
		this.maxPoints = maxPoints;
		this.nodeBudget = nodeBudget;
		this.itemsPerThread = itemsPerThread;
		this.maxItems = maxItems;
		this.minBoxes = minBoxes;
		this.maxBoxes = maxBoxes;
		this.jointTree = jointTree;
	}

	@Override
	public boolean supports(FlatProblem problem) {
		int n = problem.n();
		return n >= minBoxes && n <= maxBoxes;
	}

	@Override
	public Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		int n = problem.n();
		if(n == 0 || n > maxBoxes) {
			return Optional.empty();
		}
		int[] ints = problem.ints();
		int depth = FlatPermutation.depth(ints, threads * itemsPerThread, maxItems);
		if(depth == 0) {
			return Optional.empty();
		}
		int[] prefixes = FlatPermutation.prefixes(ints, depth, maxItems);
		if(prefixes == null) {
			return Optional.empty();
		}

		Run run = new Run(n, ints, problem.longs(), new FlatLayout(n, maxPoints), depth, prefixes, boundVolume, jointTree ? FlatTreeItem.outInts(n) : FlatSubtree.outInts(n));

		// the scratch of each thread: one scratch is reused for all the items of the thread
		int[][] scratch = new int[threads][];
		long[][] scratchLongs = new long[threads][];
		for (int t = 0; t < threads; t++) {
			scratch[t] = run.layout.newScratch();
			scratchLongs[t] = new long[run.layout.longSize];
		}

		while (true) {
			// the items which are not done, before the first which places all boxes
			int fullItem = run.flat.fullItem();
			run.fullItem.set(fullItem);
			int pendingCount = run.flat.pending(fullItem, run.pending);
			if(pendingCount == 0) {
				break;
			}
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			run.pendingCount = pendingCount;
			run.next = new AtomicInteger();

			if(threads == 1) {
				while (!run.overflow) {
					int index = run.next.getAndIncrement();
					if(index >= run.pendingCount) {
						break;
					}
					if(interrupt.getAsBoolean()) {
						throw new PackagerInterruptedException();
					}
					runItem(run, run.pending[index], scratch[0], scratchLongs[0]);
				}
			} else {
				runThreads(run, scratch, scratchLongs, interrupt);
			}
			if(run.overflow) {
				return Optional.empty();
			}
		}
		return Optional.of(run.flat.reduce());
	}

	private void runThreads(Run run, int[][] scratch, long[][] scratchLongs, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		ExecutorService executor = getExecutor();
		List<Future<?>> futures = new ArrayList<>(threads);
		for (int t = 0; t < threads; t++) {
			int[] s = scratch[t];
			long[] sl = scratchLongs[t];
			futures.add(executor.submit(() -> work(run, s, sl)));
		}

		boolean interrupted = false;
		Throwable failure = null;
		for (Future<?> future : futures) {
			while (true) {
				try {
					future.get(POLL_MILLIS, TimeUnit.MILLISECONDS);
					break;
				} catch (TimeoutException e) {
					// the interrupt is polled by the calling thread only
					if(!interrupted && interrupt.getAsBoolean()) {
						interrupted = true;
						run.abort = true;
					}
				} catch (ExecutionException e) {
					run.abort = true;
					if(failure == null) {
						failure = e.getCause();
					}
					break;
				} catch (InterruptedException e) {
					run.abort = true;
					Thread.currentThread().interrupt();
					throw new PackagerInterruptedException();
				}
			}
		}
		if(failure != null) {
			throw new IllegalStateException("Flat search failed", failure);
		}
		if(interrupted) {
			throw new PackagerInterruptedException();
		}
	}

	private void work(Run run, int[] s, long[] sl) {
		while (!run.abort && !run.overflow) {
			int index = run.next.getAndIncrement();
			if(index >= run.pendingCount) {
				return;
			}
			runItem(run, run.pending[index], s, sl);
		}
	}

	private void runItem(Run run, int item, int[] s, long[] sl) {
		if(item > run.fullItem.get()) {
			// an earlier item places all boxes
			return;
		}
		int base = item * run.outInts;
		int longBase = item * FlatSubtree.OUT_LONGS;
		int status;
		if(jointTree) {
			status = FlatTreeItem.run(run.layout, run.problem, run.problemLongs, run.depth, run.prefixes, item, s, 0, run.out, base, run.outLongs, longBase, run.bound.get(), nodeBudget);
		} else {
			status = FlatSubtree.run(run.layout, run.problem, run.problemLongs, run.depth, run.prefixes, item, s, sl, 0, 0, run.out, base, run.outLongs, longBase, run.bound.get(), nodeBudget);
		}
		if(status == FlatSubtree.OVERFLOW) {
			run.overflow = true;
			return;
		}
		if(run.out[base + FlatSubtree.OUT_COUNT] > 0) {
			// a load volume which another permutation reaches: permutations which cannot load more need not be searched
			run.bound.accumulateAndGet(run.outLongs[longBase + FlatSubtree.OUT_VOLUME], Math::max);
		}
		if(status == FlatSubtree.DONE && run.out[base + FlatSubtree.OUT_FULL] != 0) {
			run.fullItem.accumulateAndGet(item, Math::min);
		}
	}

	private synchronized ExecutorService getExecutor() {
		if(closed) {
			throw new IllegalStateException("The engine is closed");
		}
		if(executor == null) {
			executor = Executors.newFixedThreadPool(threads, runnable -> {
				Thread thread = new Thread(runnable, "packing-flat-engine");
				thread.setDaemon(true);
				return thread;
			});
		}
		return executor;
	}

	@Override
	public synchronized void close() {
		closed = true;
		if(executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}
}
