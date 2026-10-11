package com.github.skjolber.packing.packer.strategy.ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

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
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

class ParallelContainerPackingStrategyTest {

	@Test
	void acceptsTheBestResultProducedByASessionFork() {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		PlainPackager packager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator))
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
			// <figure>
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |  box  |  box  |         |  box  |  box  |         |  box  |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |  box  |  box  | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			// </figure>
			figure(result);

			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(result.getContainers()).singleElement().extracting(Container::getId).isEqualTo("large");
		} finally {
			packager.close();
			executorService.shutdownNow();
		}
	}

	/**
	 * Two container types of the same size, so that every attempt packs the same result and the comparator cannot tell the
	 * candidates apart. The container items are in preference order, so the more preferred container type (A) must be used,
	 * as {@linkplain OrderedContainerPackingStrategy} does.
	 *
	 * <p>The outcome does not depend on which task finishes first: {@linkplain ParallelContainerPackingStrategy} submits one
	 * task for each candidate, and then collects the results by waiting for the futures in container item order (not in the order
	 * the tasks finish), comparing each result with the best so far. Every task packs its own fork of the session, so its result
	 * does not depend on the others either. The ties are therefore always decided in preference order, and the test repeats the
	 * packaging to show it.</p>
	 */
	@Test
	void equalResultsKeepTheMostPreferredContainer() {
		//  two unit cubes, A and B are the same size: both cubes go to A, one in each container
		//
		//   A [a]   A [b]
		//
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		PlainPackager parallel = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator))
				.build();
		PlainPackager ordered = PlainPackager.newBuilder().build();
		try {
			Container a = Container.newBuilder().withId("A").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container b = Container.newBuilder().withId("B").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Box boxA = Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build();
			Box boxB = Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build();

			for (int i = 0; i < 20; i++) {
				for (PlainPackager packager : List.of(parallel, ordered)) {
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(List.of(new ContainerItem(a, 2), new ContainerItem(b, 2)))
							.withMaxContainerCount(2)
							.withBoxItems(new BoxItem(boxA, 1), new BoxItem(boxB, 1))
							.withInterruptDuration(10_000)
							.build();
					// <figure>
					// container 1 of 2: A
					//   z                 z                 y                 z
					//                     1 +-------+       1 +-------+       1 +-------+
					//   | /-------|   y     |       |         |       |         |       |
					//   |/       /|         |   a   |         |   a   |         |   a   |
					// 1 |-------| | /       |       |         |       |         |       |
					//   |       | |/      0 +-------+       0 +-------+       0 +-------+
					//   |   a   | | 1       0       1   x     0       1   x     0       1   y
					//   |       |/
					// 0 |-------|-- x
					//   0       1
					//
					// container 2 of 2: A
					//   z                 z                 y                 z
					//                     1 +-------+       1 +-------+       1 +-------+
					//   | /-------|   y     |       |         |       |         |       |
					//   |/       /|         |   b   |         |   b   |         |   b   |
					// 1 |-------| | /       |       |         |       |         |       |
					//   |       | |/      0 +-------+       0 +-------+       0 +-------+
					//   |   b   | | 1       0       1   x     0       1   x     0       1   y
					//   |       |/
					// 0 |-------|-- x
					//   0       1
					// </figure>
					figure(result);

					PackagerResultAssert.assertThat(result).isSuccess();
					assertThat(result.getContainers()).extracting(Container::getId).as(packager == parallel ? "parallel" : "ordered").containsExactly("A", "A");
				}
			}
		} finally {
			parallel.close();
			ordered.close();
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
				BruteForcePackager.newBuilder().withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator)).build(),
				FastBruteForcePackager.newBuilder().withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator)).build());
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
				// <figure>
				// container 1 of 3: large
				//   z                         z                         y                         z
				//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
				//   | /-------/-------|   y     |       |       |         |       |       |         |       |
				//   |/       /       /|         |   a   |   b   |         |   a   |   b   |         |   b   |
				// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
				//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
				//   |   a   |   b   | | 1       0       1       2   x     0       1       2   x     0       1   y
				//   |       |       |/
				// 0 |-------|-------|-- x
				//   0       1       2
				//
				// container 2 of 3: small
				//   z                 z                 y                 z
				//                     1 +-------+       1 +-------+       1 +-------+
				//   | /-------|   y     |       |         |       |         |       |
				//   |/       /|         |   c   |         |   c   |         |   c   |
				// 1 |-------| | /       |       |         |       |         |       |
				//   |       | |/      0 +-------+       0 +-------+       0 +-------+
				//   |   c   | | 1       0       1   x     0       1   x     0       1   y
				//   |       |/
				// 0 |-------|-- x
				//   0       1
				//
				// container 3 of 3: small
				//   z                 z                 y                 z
				//                     1 +-------+       1 +-------+       1 +-------+
				//   | /-------|   y     |       |         |       |         |       |
				//   |/       /|         |   d   |         |   d   |         |   d   |
				// 1 |-------| | /       |       |         |       |         |       |
				//   |       | |/      0 +-------+       0 +-------+       0 +-------+
				//   |   d   | | 1       0       1   x     0       1   x     0       1   y
				//   |       |/
				// 0 |-------|-- x
				//   0       1
				// </figure>
				figure(result);

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
	 * The parallel container packing strategy attempts containers in session forks at the same time; with parallel brute force,
	 * each attempt splits its permutations between workers. The attempts must not take each other's workers' results.
	 */
	@Test
	void parallelBruteForceAttemptsContainersAtTheSameTime() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		try (DefaultValidator validator = new DefaultValidator();
				ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
						.withThreads(4)
						.withParallelizationCount(2)
						.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator))
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
