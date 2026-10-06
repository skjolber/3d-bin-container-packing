package com.github.skjolber.packing.packer.strategy.ordered;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

class ParallelContainerPackingStrategyTest {

	@Test
	void acceptsTheBestResultProducedByASessionFork() {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		PlainPackager packager = PlainPackager.newBuilder()
				.withContainerStrategyFactory((inventory, boxes, groups) -> new ParallelContainerPackingStrategy(executorService, new DefaultIntermediatePackagerResultComparator()))
				.build();
		try {

			Container small = Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container large = Container.newBuilder().withId("large").withSize(2, 1, 1).withMaxLoadWeight(2).build();
			Box box = Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(small, 2), new ContainerItem(large, 1)))
					.withMaxContainerCount(2)
					.withBoxItems(new BoxItem(box, 2))
					.build();

			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(result.getContainers()).singleElement().extracting(Container::getId).isEqualTo("large");
		} finally {
			packager.close();
			executorService.shutdownNow();
		}
	}

	//
	//  four unit cubes; one large container (2 x 1 x 1) and four small (1 x 1 x 1). The results of the session forks are
	//  accepted on the session itself, which must count the large container as used:
	//
	//   [a][b]   [c]   [d]
	//
	@Test
	void bruteForceUsesEachContainerOnce() {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		List<AbstractPackager<?>> packagers = List.of(
				BruteForcePackager.newBuilder().withContainerStrategyFactory((inventory, boxes, groups) -> new ParallelContainerPackingStrategy(executorService, new DefaultIntermediatePackagerResultComparator())).build(),
				FastBruteForcePackager.newBuilder().withContainerStrategyFactory((inventory, boxes, groups) -> new ParallelContainerPackingStrategy(executorService, new DefaultIntermediatePackagerResultComparator())).build());
		try {
			for (AbstractPackager<?> packager : packagers) {
				Container small = Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(10).build();
				Container large = Container.newBuilder().withId("large").withSize(2, 1, 1).withMaxLoadWeight(10).build();
				List<BoxItem> boxItems = new ArrayList<>();
				for (String id : List.of("a", "b", "c", "d")) {
					boxItems.add(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1));
				}

				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(small, 4), new ContainerItem(large, 1)))
						.withMaxContainerCount(4)
						.withBoxItems(boxItems)
						.build();

				PackagerResultAssert.assertThat(result).isSuccess();
				assertThat(result.getContainers()).as(packager.getClass().getSimpleName()).extracting(Container::getId).containsOnlyOnce("large");
			}
		} finally {
			for (AbstractPackager<?> packager : packagers) {
				packager.close();
			}
			executorService.shutdownNow();
		}
	}

	/**
	 * The parallel container strategy attempts containers in session forks at the same time; with parallel brute force,
	 * each attempt splits its permutations between workers. The attempts must not take each other's workers' results.
	 */
	@Test
	void parallelBruteForceAttemptsContainersAtTheSameTime() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		try (DefaultValidator validator = new DefaultValidator();
				ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
						.withThreads(4)
						.withParallelizationCount(2)
						.withContainerStrategyFactory((inventory, boxes, groups) -> new ParallelContainerPackingStrategy(executorService, new DefaultIntermediatePackagerResultComparator()))
						.build()) {
			for (int seed = 0; seed < 20; seed++) {
				Random random = new Random(seed);
				List<BoxItem> boxItems = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					boxItems.add(new BoxItem(Box.newBuilder().withId("b" + i).withSize(1 + random.nextInt(2), 1 + random.nextInt(2), 1).withWeight(1).build(), 1));
				}
				List<ContainerItem> containers = List.of(
						new ContainerItem(Container.newBuilder().withId("small").withSize(2, 2, 1).withMaxLoadWeight(10).build(), 5),
						new ContainerItem(Container.newBuilder().withId("medium").withSize(3, 2, 1).withMaxLoadWeight(10).build(), 2),
						new ContainerItem(Container.newBuilder().withId("large").withSize(4, 2, 1).withMaxLoadWeight(10).build(), 1));

				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers)
						.withMaxContainerCount(5)
						.withBoxItems(boxItems)
						.withInterruptDuration(10_000)
						.build();

				PackagerResultAssert.assertThat(result).isSuccess();
				PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
						.withContainerItems(containers)
						.withMaxContainerCount(5)
						.withBoxItems(boxItems));
			}
		} finally {
			executorService.shutdownNow();
		}
	}
}
