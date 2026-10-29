package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.UnaryOperator;

import com.github.skjolber.packing.api.PackagerException;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.iterator.AbstractBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * The search of a container's orders as one tree on several threads, with work stealing: the first thread searches the whole tree, the others
 * steal the shallowest subtree a thread has still to search (see {@link BruteForceTreeSearch.StealableState}), and so on until no thread has
 * work. The threads share their best arrangement for pruning; a complete result stops the others. The results compare by the result comparator,
 * then by their orders. The packagers supply the search of a part on a thread's own state ({@link Part}).
 */
final class BruteForceTreeStealing {

	/** the search of a part of the tree on a thread's own point calculator and placements */
	interface Part {

		BruteForceIntermediatePackagerResult search(int task, AbstractBoxItemPermutationRotationIterator threadIterator, PackagerInterruptSupplier interrupt,
				BruteForceTreeSearch.Work work, BruteForceTreeSearch.SharedBest shared, BruteForceTreeSearch.Tables[] tables, BruteForceTreeSearch.StealableState state)
				throws PackagerInterruptedException;
	}

	private BruteForceTreeStealing() {
	}

	/**
	 * @param tasks the number of threads to search on
	 * @param iterator the container's iterator, which the threads fork
	 * @param completeBoxCount the number of boxes of a result which no other result can beat
	 * @param truncate the truncation of a result, or null
	 * @param wrap wraps a thread's task, for example to set a thread priority, or the identity
	 */
	static BruteForceIntermediatePackagerResult search(ExecutorService executorService, int tasks, AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts,
			PackagerInterruptSupplier sourceInterrupt, int completeBoxCount, UnaryOperator<BruteForceIntermediatePackagerResult> truncate,
			IntermediatePackagerResultComparator comparator, Part part, UnaryOperator<Callable<BruteForceIntermediatePackagerResult>> wrap) throws PackagerInterruptedException {
		int[] counts = new int[iterator.getBoxItemStackValues().length];
		for (int value : iterator.getPermutations()) {
			counts[value]++;
		}
		int[] groupEnd = BruteForceTreeSearch.groupEnds(iterator.getPermutations(), insertAfterCounts, counts.length);
		BruteForceTreeSearch.StealableState[] states = new BruteForceTreeSearch.StealableState[tasks];
		for (int j = 0; j < tasks; j++) {
			states[j] = new BruteForceTreeSearch.StealableState();
		}
		// the parts being searched, or stolen and about to be: the search is done when there are none
		AtomicInteger active = new AtomicInteger(1);
		AtomicBoolean complete = new AtomicBoolean();
		AtomicBoolean failed = new AtomicBoolean();
		BruteForceTreeSearch.SharedBest shared = new BruteForceTreeSearch.SharedBest();
		PackagerInterruptSupplier interrupt = () -> failed.get() || complete.get() || sourceInterrupt.getAsBoolean();

		ExecutorCompletionService<BruteForceIntermediatePackagerResult> executorCompletionService = new ExecutorCompletionService<>(executorService);
		List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(tasks);
		for (int j = 0; j < tasks; j++) {
			int task = j;
			BruteForceTreeSearch.StealableState state = states[j];
			futures.add(executorCompletionService.submit(wrap.apply(() -> {
				AbstractBoxItemPermutationRotationIterator threadIterator = iterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator ? defaultIterator.fork()
						: ((DefaultBoxItemGroupPermutationRotationIterator)iterator).fork();
				BruteForceTreeSearch.Tables[] tables = new BruteForceTreeSearch.Tables[1];
				BruteForceIntermediatePackagerResult best = null;
				BruteForceTreeSearch.Work work = task == 0 ? BruteForceTreeSearch.Work.ROOT : null;
				while (true) {
					if(work == null) {
						if(interrupt.getAsBoolean()) {
							break;
						}
						// steal from the thread with the shallowest work, or wait for one to have some
						work = steal(states, state, counts, groupEnd, active);
						if(work == null) {
							if(active.get() == 0) {
								break;
							}
							LockSupport.parkNanos(50_000L);
							continue;
						}
					}
					int[] order = BruteForceTreeSearch.firstOrder(threadIterator, work.prefixTypes);
					if(threadIterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator) {
						defaultIterator.setPermutation(order);
					} else {
						((DefaultBoxItemGroupPermutationRotationIterator)threadIterator).setPermutation(order);
					}
					try {
						BruteForceIntermediatePackagerResult result = part.search(task, threadIterator, interrupt, work, shared, tables, state);
						if(truncate != null) {
							result = truncate.apply(result);
						}
						if(result != null && !result.isEmpty() && (best == null || compare(comparator, best, result) < 0)) {
							best = result;
						}
						if(result != null && result.getBoxCount() >= completeBoxCount) {
							complete.set(true);
						}
					} catch (PackagerInterruptedException e) {
						if(!complete.get()) {
							throw e;
						}
						// stopped: another thread is complete
					} finally {
						synchronized (state.lock) {
							state.active = false;
							state.depth = 0;
						}
						active.decrementAndGet();
					}
					work = null;
				}
				return best;
			})));
		}
		BruteForceIntermediatePackagerResult best = null;
		try {
			for (int j = 0; j < futures.size(); j++) {
				try {
					BruteForceIntermediatePackagerResult result = executorCompletionService.take().get();
					if(result != null && (best == null || compare(comparator, best, result) < 0)) {
						best = result;
					}
				} catch (ExecutionException e) {
					failed.set(true);
					Throwable cause = e.getCause();
					if(cause instanceof PackagerInterruptedException) {
						throw (PackagerInterruptedException)cause;
					}
					throw new PackagerException(cause);
				} catch (InterruptedException e) {
					failed.set(true);
					throw new PackagerInterruptedException();
				}
			}
		} finally {
			for (Future<BruteForceIntermediatePackagerResult> future : futures) {
				future.cancel(true);
			}
		}
		if(sourceInterrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		return best;
	}

	/** steal work from another thread: the shallowest work of any (the thief's own state is not active) */
	private static BruteForceTreeSearch.Work steal(BruteForceTreeSearch.StealableState[] states, BruteForceTreeSearch.StealableState own, int[] counts, int[] groupEnd,
			AtomicInteger active) {
		for (BruteForceTreeSearch.StealableState state : states) {
			if(state == own) {
				continue;
			}
			BruteForceTreeSearch.Work work = state.steal(counts, groupEnd, active);
			if(work != null) {
				return work;
			}
		}
		return null;
	}

	/**
	 * @return the comparison of two results of parts of a search, as on one thread: by the result comparator, then the first order
	 */
	private static int compare(IntermediatePackagerResultComparator comparator, BruteForceIntermediatePackagerResult a, BruteForceIntermediatePackagerResult b) {
		int compare = comparator.compare(a, b);
		if(compare != 0) {
			return compare;
		}
		// equal: the earlier order wins, i.e. compares as the better (greater)
		return -Arrays.compare(a.getPermutationRotationIteratorForState().getPermutations(), b.getPermutationRotationIteratorForState().getPermutations());
	}
}
