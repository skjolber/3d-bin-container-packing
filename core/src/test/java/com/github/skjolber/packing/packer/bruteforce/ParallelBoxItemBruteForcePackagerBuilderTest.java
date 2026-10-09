package com.github.skjolber.packing.packer.bruteforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

/**
 * Thread and parallelization settings of {@linkplain ParallelBoxItemBruteForcePackager.ParallelBruteForcePackagerBuilder}.
 */

public class ParallelBoxItemBruteForcePackagerBuilderTest extends AbstractPackagerTest {

	// six unit cubes in a row; small enough for brute force, with enough permutations to be split into work units
	//
	//   [a][b][c][d][e][f]
	//
	private void assertPacksSixCubesInARow(ParallelBoxItemBruteForcePackager packager) {
		List<BoxItem> products = new ArrayList<>();
		for (String id : new String[] { "a", "b", "c", "d", "e", "f" }) {
			products.add(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1));
		}
		Container container = Container.newBuilder().withDescription("1").withSize(6, 1, 1).withMaxLoadWeight(100).build();

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(ContainerItem.newListBuilder().withContainer(container, 1).build())
				.withBoxItems(products)
				.withMaxContainerCount(1)
				.withDeadline(System.currentTimeMillis() + 10_000)
				.build();

		assertTrue(result.isSuccess());
		assertValid(result);
		assertEquals(products.size(), result.get(0).getStack().size());
	}

	@Test
	void cachedThreadPoolWithoutParallelizationCountPacks() {
		// the maximum pool size of a cached thread pool is Integer.MAX_VALUE
		ExecutorService executor = Executors.newCachedThreadPool();
		try {
			ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
					.withExecutorService(executor)
					.build();
			try {
				assertPacksSixCubesInARow(packager);
			} finally {
				packager.close();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void boundedThreadPoolWithoutParallelizationCountPacks() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
					.withExecutorService(executor)
					.build();
			try {
				assertPacksSixCubesInARow(packager);
			} finally {
				packager.close();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void availableProcessorsFactorMustBePositive() {
		ParallelBoxItemBruteForcePackager.ParallelBruteForcePackagerBuilder builder = ParallelBoxItemBruteForcePackager.newBuilder();

		assertThrows(IllegalArgumentException.class, () -> builder.withAvailableProcessors(0));
		assertThrows(IllegalArgumentException.class, () -> builder.withAvailableProcessors(-1));
		assertThrows(IllegalArgumentException.class, () -> builder.withAvailableProcessors(Integer.MIN_VALUE));
	}

	@Test
	void availableProcessorsFactorLargerThanTheProcessorCountStillGivesOneThread() {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withAvailableProcessors(Integer.MAX_VALUE)
				.build();
		try {
			assertEquals(1, ((ThreadPoolExecutor)packager.getExecutorService()).getMaximumPoolSize());

			assertPacksSixCubesInARow(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	void availableProcessorsFactorOneUsesAllProcessors() {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withAvailableProcessors(1)
				.build();
		try {
			assertEquals(Runtime.getRuntime().availableProcessors(), ((ThreadPoolExecutor)packager.getExecutorService()).getMaximumPoolSize());

			assertPacksSixCubesInARow(packager);
		} finally {
			packager.close();
		}
	}
}
