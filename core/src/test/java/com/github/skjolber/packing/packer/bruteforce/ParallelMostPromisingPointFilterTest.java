package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;

class ParallelMostPromisingPointFilterTest {

	@Test
	void appliesTheDynamicPointLimitInParallelPermutationSearch() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		AtomicInteger invocations = new AtomicInteger();
		AtomicBoolean invokedOnWorker = new AtomicBoolean();
		Thread callingThread = Thread.currentThread();
		BruteForcePackager.BruteForcePointIteratorFilter filter = new BruteForcePackager.MostPromisingPointFilter(
				(points, stackValue) -> {
					invocations.incrementAndGet();
					invokedOnWorker.compareAndSet(false, Thread.currentThread() != callingThread);
					return 1;
				});
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.withPointFilter(filter)
				.build();
		try {
			Container container = Container.newBuilder().withSize(6, 1, 1).withMaxLoadWeight(3).build();
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withBoxItems(List.of(
							new BoxItem(Box.newBuilder().withId("one").withSize(1, 1, 1).withWeight(1).build()),
							new BoxItem(Box.newBuilder().withId("two").withSize(2, 1, 1).withWeight(1).build()),
							new BoxItem(Box.newBuilder().withId("three").withSize(3, 1, 1).withWeight(1).build())))
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(invocations).hasValueGreaterThanOrEqualTo(3);
			assertThat(invokedOnWorker).isTrue();
		} finally {
			try {
				packager.close();
			} finally {
				executor.shutdownNow();
			}
		}
	}
}
