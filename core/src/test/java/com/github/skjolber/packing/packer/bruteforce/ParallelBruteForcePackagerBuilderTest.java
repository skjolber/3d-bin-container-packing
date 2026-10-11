package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;

public class ParallelBruteForcePackagerBuilderTest {

	@Test
	void unboundedExecutorServiceGetsPositiveParallelizationCount() {
		ExecutorService executor = Executors.newCachedThreadPool();
		try {
			ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executor).build();
			try {
				assertThat(packager.getParallelizationCount()).isEqualTo(16 * Runtime.getRuntime().availableProcessors());

				assertPacks(packager);
			} finally {
				packager.close();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void boundedExecutorServiceGetsParallelizationCountFromPoolSize() {
		ExecutorService executor = Executors.newFixedThreadPool(3);
		try {
			ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executor).build();
			try {
				assertThat(packager.getParallelizationCount()).isEqualTo(48);
			} finally {
				packager.close();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void availableProcessorsFactorMustBePositive() {
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withAvailableProcessors(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withAvailableProcessors(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void availableProcessorsFactorAboveProcessorCountStillGivesOneThread() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withAvailableProcessors(10000).build();
		try {
			assertThat(packager.getParallelizationCount()).isEqualTo(16);

			assertPacks(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	void parallelizationCountMustBePositive() {
		assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withParallelizationCount(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void threadsAndExecutorServiceAreMutuallyExclusive() {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		try {
			assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withThreads(2).withExecutorService(executor).build())
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Not expecting both thread count and executor service");
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void customExecutorServiceRequiresParallelizationCount() {
		// not a ThreadPoolExecutor, so the parallelization count cannot be detected
		ExecutorService executor = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));
		try {
			assertThatThrownBy(() -> ParallelBruteForcePackager.newBuilder().withExecutorService(executor).build())
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Expected a parallelization count for custom executor service");

			ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executor).withParallelizationCount(8).build();
			try {
				assertThat(packager.getParallelizationCount()).isEqualTo(8);

				assertPacks(packager);
			} finally {
				packager.close();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void builderCanBuildSeveralPackagers() {
		ParallelBruteForcePackager.Builder builder = ParallelBruteForcePackager.newBuilder().withThreads(2);

		ParallelBruteForcePackager first = builder.build();
		try {
			ParallelBruteForcePackager second = builder.build();
			try {
				assertThat(second.getParallelizationCount()).isEqualTo(32);

				assertPacks(first);
				assertPacks(second);
			} finally {
				second.close();
			}
		} finally {
			first.close();
		}
	}

	private static void assertPacks(ParallelBruteForcePackager packager) {
		List<BoxItem> boxItems = new ArrayList<>();
		for(int i = 0; i < 5; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId("box-" + i).withSize(1, 1, 1).withWeight(1).build(), 1));
		}
		ContainerItem containerItem = new ContainerItem(Container.newBuilder().withId("container").withSize(5, 1, 1).withMaxLoadWeight(100).build(), 1);

		PackagerResult result = packager.newResultBuilder()
				.withContainerItem(containerItem)
				.withBoxItems(boxItems)
				.withInterruptDuration(60_000L)
				.build();

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.get(0).getStack().size()).isEqualTo(5);
	}
}
