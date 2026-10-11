package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.iterator.IntIterator;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.DefaultPointFilter;

/**
 * The thread priority option of the parallel brute-force packager: every thread which searches (the pool threads, and the calling thread when it
 * searches itself) runs at the priority while it searches, and gets its original priority back afterwards. A point filter, which the search calls
 * for every candidate position, records the priority of the thread which calls it.
 */
public class ParallelBruteForcePackagerThreadPriorityTest {

	/** The configured priority: neither the normal priority, nor the original priority of the pool threads */
	private static final int PRIORITY = Thread.MIN_PRIORITY + 1;

	/** The original priority of the pool threads of the tests */
	private static final int POOL_PRIORITY = Thread.NORM_PRIORITY - 1;

	private static final long INTERRUPT_DURATION = 60_000L;

	/**
	 * Point filter which records the priorities of the threads which call it, separately for the thread which created it (the thread which calls
	 * the packager), and for the other threads (the workers).
	 */
	private static class PriorityRecordingPointFilter implements BruteForcePointIteratorFilter {

		private final Thread callingThread = Thread.currentThread();
		private final BruteForcePointIteratorFilter delegate = new DefaultPointFilter();

		final Set<Integer> callerPriorities = ConcurrentHashMap.newKeySet();
		final Set<Integer> workerPriorities = ConcurrentHashMap.newKeySet();
		final AtomicInteger callerCalls = new AtomicInteger();
		final AtomicInteger workerCalls = new AtomicInteger();

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
			record();
			return delegate.getPoints(pointCalculator, stackValue);
		}

		protected void record() {
			Thread thread = Thread.currentThread();
			if(thread == callingThread) {
				callerPriorities.add(thread.getPriority());
				callerCalls.incrementAndGet();
			} else {
				workerPriorities.add(thread.getPriority());
				workerCalls.incrementAndGet();
			}
		}
	}

	/** Point filter which records the priority and then fails, on the calling thread and on the workers alike */
	private static class FailingPointFilter extends PriorityRecordingPointFilter {

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
			record();
			throw new IllegalStateException("Expected failure");
		}
	}

	private record PoolThread(String name, int priority) {
	}

	@Test
	void workersSearchAtThePriorityAndRestoreIt() throws Exception {
		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			// 5 boxes of different sizes: 120 permutations, more than the 4 which a single thread takes
			assertPacksBoxItems(packager, 5);

			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(PRIORITY);
			// the calling thread only waits for the workers
			assertThat(filter.callerCalls.get()).isZero();

			// the pool threads are back to their original priority
			for (PoolThread poolThread : probe(executor, 2)) {
				assertThat(poolThread.priority()).isEqualTo(POOL_PRIORITY);
			}
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void workersOfASingleBoxItemGroupSearchAtThePriority() throws Exception {
		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			// one group: its permutations are split between the workers
			assertPacksBoxItemGroups(packager, 1, 5);

			assertThat(packager.groupOrderSplits.get()).isZero();
			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(PRIORITY);

			for (PoolThread poolThread : probe(executor, 2)) {
				assertThat(poolThread.priority()).isEqualTo(POOL_PRIORITY);
			}
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void workersOfFewBoxItemGroupOrdersSearchAtThePriority() throws Exception {
		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(16)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			// two groups have two orders, too few to split between the workers: each order is searched in turn, with its permutations split between the workers
			assertPacksBoxItemGroups(packager, 2, 3);

			assertThat(packager.groupOrderSplits.get()).isZero();
			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(PRIORITY);

			for (PoolThread poolThread : probe(executor, 2)) {
				assertThat(poolThread.priority()).isEqualTo(POOL_PRIORITY);
			}
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void workersOfSplitBoxItemGroupOrdersSearchAtThePriority() throws Exception {
		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(4)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			// the orders of the groups are split between the workers (by their first groups)
			assertPacksBoxItemGroups(packager, 3, 2);

			assertThat(packager.groupOrderSplits.get()).isGreaterThan(0);
			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(PRIORITY);

			for (PoolThread poolThread : probe(executor, 2)) {
				assertThat(poolThread.priority()).isEqualTo(POOL_PRIORITY);
			}
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void callingThreadSearchesAtThePriorityAndRestoresIt() {
		int original = Thread.currentThread().getPriority();
		assertThat(original).isNotEqualTo(PRIORITY);

		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			// 2 permutations, fewer than the 4 which a single thread takes: no tasks for the workers
			assertPacksBoxItems(packager, 2);

			assertThat(filter.workerCalls.get()).isZero();
			assertThat(filter.callerCalls.get()).isGreaterThan(0);
			assertThat(filter.callerPriorities).containsExactly(PRIORITY);
			assertThat(Thread.currentThread().getPriority()).isEqualTo(original);
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void callingThreadSearchesBoxItemGroupsAtThePriorityAndRestoresIt() {
		int original = Thread.currentThread().getPriority();
		assertThat(original).isNotEqualTo(PRIORITY);

		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			assertPacksBoxItemGroups(packager, 1, 2);

			assertThat(filter.workerCalls.get()).isZero();
			assertThat(filter.callerCalls.get()).isGreaterThan(0);
			assertThat(filter.callerPriorities).containsExactly(PRIORITY);
			assertThat(Thread.currentThread().getPriority()).isEqualTo(original);
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void callingThreadPriorityIsRestoredAfterAFailure() {
		int original = Thread.currentThread().getPriority();

		ExecutorService executor = newExecutor(2);
		FailingPointFilter filter = new FailingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			assertThatThrownBy(() -> pack(packager, 2)).hasStackTraceContaining("Expected failure");

			assertThat(filter.callerPriorities).containsExactly(PRIORITY);
			assertThat(Thread.currentThread().getPriority()).isEqualTo(original);
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void workerPriorityIsRestoredAfterAFailure() throws Exception {
		ExecutorService executor = newExecutor(2);
		FailingPointFilter filter = new FailingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			assertThatThrownBy(() -> pack(packager, 5)).hasStackTraceContaining("Expected failure");

			assertThat(filter.workerPriorities).containsExactly(PRIORITY);
			for (PoolThread poolThread : probe(executor, 2)) {
				assertThat(poolThread.priority()).isEqualTo(POOL_PRIORITY);
			}
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void withoutThreadPriorityTheThreadsKeepTheirPriority() throws Exception {
		int original = Thread.currentThread().getPriority();

		ExecutorService executor = newExecutor(2);
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.build();
		try {
			assertThat(packager.getThreadPriority()).isEqualTo(-1);

			// the workers
			assertPacksBoxItems(packager, 5);
			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(POOL_PRIORITY);

			// the calling thread
			assertPacksBoxItems(packager, 2);
			assertThat(filter.callerCalls.get()).isGreaterThan(0);
			assertThat(filter.callerPriorities).containsExactly(original);

			// box item groups, on the workers
			assertPacksBoxItemGroups(packager, 3, 2);
			assertThat(packager.groupOrderSplits.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(POOL_PRIORITY);

			assertThat(Thread.currentThread().getPriority()).isEqualTo(original);
		} finally {
			close(packager, executor);
		}
	}

	@Test
	void builderCreatedExecutorServiceCreatesThreadsAtThePriority() throws Exception {
		PriorityRecordingPointFilter filter = new PriorityRecordingPointFilter();
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withThreads(2)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.withThreadPriority(PRIORITY)
				.build();
		try {
			assertThat(packager.getThreadPriority()).isEqualTo(PRIORITY);

			// plain tasks: the threads were created at the priority
			for (PoolThread poolThread : probe(packager.getExecutorService(), 2)) {
				assertThat(poolThread.priority()).isEqualTo(PRIORITY);
				assertThat(poolThread.name()).startsWith("3d-packaging-thread-");
			}

			assertPacksBoxItems(packager, 5);
			assertThat(filter.workerCalls.get()).isGreaterThan(0);
			assertThat(filter.workerPriorities).containsExactly(PRIORITY);

			// still at the priority
			for (PoolThread poolThread : probe(packager.getExecutorService(), 2)) {
				assertThat(poolThread.priority()).isEqualTo(PRIORITY);
			}
		} finally {
			packager.close();
		}
	}

	@Test
	void builderCreatedExecutorServiceKeepsTheNormalPriorityByDefault() throws Exception {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).build();
		try {
			assertThat(packager.getThreadPriority()).isEqualTo(-1);

			for (PoolThread poolThread : probe(packager.getExecutorService(), 2)) {
				assertThat(poolThread.priority()).isEqualTo(Thread.NORM_PRIORITY);
			}
		} finally {
			packager.close();
		}
	}

	@Test
	void threadPriorityMustBeWithinTheRange() {
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreadPriority(Thread.MIN_PRIORITY - 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreadPriority(Thread.MAX_PRIORITY + 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreadPriority(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreadPriority(11)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreadPriority(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void threadPriorityBoundsAreAccepted() {
		for (int priority : new int[] { Thread.MIN_PRIORITY, Thread.MAX_PRIORITY }) {
			ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(1).withThreadPriority(priority).build();
			try {
				assertThat(packager.getThreadPriority()).isEqualTo(priority);

				assertPacksBoxItems(packager, 3);
			} finally {
				packager.close();
			}
		}
	}

	@Test
	void defaultThreadFactoryCreatesThreadsAtThePriority() {
		assertThat(new DefaultThreadFactory().newThread(() -> {
		}).getPriority()).isEqualTo(Thread.NORM_PRIORITY);

		DefaultThreadFactory factory = new DefaultThreadFactory(Thread.MIN_PRIORITY);
		Thread first = factory.newThread(() -> {
		});
		Thread second = factory.newThread(() -> {
		});
		assertThat(first.getPriority()).isEqualTo(Thread.MIN_PRIORITY);
		assertThat(second.getPriority()).isEqualTo(Thread.MIN_PRIORITY);
		assertThat(first.getName()).isEqualTo("3d-packaging-thread-1");
		assertThat(second.getName()).isEqualTo("3d-packaging-thread-2");

		assertThatThrownBy(() -> new DefaultThreadFactory(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DefaultThreadFactory(11)).isInstanceOf(IllegalArgumentException.class);
	}

	/** @return a pool whose threads have the original priority {@linkplain #POOL_PRIORITY} */
	private static ExecutorService newExecutor(int threads) {
		ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			thread.setPriority(POOL_PRIORITY);
			return thread;
		};
		return Executors.newFixedThreadPool(threads, threadFactory);
	}

	/**
	 * Run a plain task on each thread of a pool (each task waits for the others, so no thread runs two).
	 *
	 * @return the pool threads, with their priorities
	 */
	private static List<PoolThread> probe(ExecutorService executor, int threads) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(threads);
		List<Future<PoolThread>> futures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			futures.add(executor.submit(() -> {
				barrier.await(30, TimeUnit.SECONDS);
				Thread thread = Thread.currentThread();
				return new PoolThread(thread.getName(), thread.getPriority());
			}));
		}
		List<PoolThread> poolThreads = new ArrayList<>();
		for (Future<PoolThread> future : futures) {
			poolThreads.add(future.get(30, TimeUnit.SECONDS));
		}
		assertThat(poolThreads).hasSize(threads);
		return poolThreads;
	}

	private static void close(ParallelBruteForcePackager packager, ExecutorService executor) {
		try {
			packager.close();
		} finally {
			executor.shutdownNow();
		}
	}

	private static PackagerResult pack(ParallelBruteForcePackager packager, int boxes) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < boxes; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId("box-" + i).withSize(i + 1, 1, 1).withWeight(1).build(), 1));
		}
		return packager.newResultBuilder()
				.withContainerItem(newContainerItem())
				.withBoxItems(boxItems)
				.withInterruptDuration(INTERRUPT_DURATION)
				.build();
	}

	private static void assertPacksBoxItems(ParallelBruteForcePackager packager, int boxes) {
		PackagerResult result = pack(packager, boxes);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
		assertThat(result.get(0).getStack().size()).isEqualTo(boxes);
	}

	private static void assertPacksBoxItemGroups(ParallelBruteForcePackager packager, int groupCount, int boxesPerGroup) {
		List<BoxItemGroup> groups = new ArrayList<>();
		int index = 0;
		for (int g = 0; g < groupCount; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			for (int i = 0; i < boxesPerGroup; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId("box-" + index).withSize(index + 1, 1, 1).withWeight(1).build(), 1));
				index++;
			}
			groups.add(new BoxItemGroup("group-" + g, boxItems));
		}

		PackagerResult result = packager.newResultBuilder()
				.withContainerItem(newContainerItem())
				.withBoxItemGroups(groups)
				.withInterruptDuration(INTERRUPT_DURATION)
				.build();

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
		assertThat(result.get(0).getStack().size()).isEqualTo(index);
	}

	private static ContainerItem newContainerItem() {
		return new ContainerItem(Container.newBuilder().withId("container").withSize(60, 1, 1).withMaxLoadWeight(100).build(), 1);
	}
}
