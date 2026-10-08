package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.test.example.BackToFrontPlacementComparator;
import com.github.skjolber.packing.test.example.LargestContainerFirstStrategy;

/**
 * Extensions which depend on the api module only (see the test module's {@code example} package), configured
 * on the core packagers.
 */
class ApiExtensionTest {

	@Test
	void largestContainerFirstStrategyUsesTheLargestContainerAlthoughASmallerOneFits() {
		// The default strategy uses the first container (in preference order) which holds all boxes.
		//
		//  default:   small [a]       largest first:   large [a][ ]
		//
		for(AbstractPackager<?> packager : largestContainerFirstPackagers()) {
			try {
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container("small", 1), 1), new ContainerItem(container("large", 2), 1)))
						.withBoxItems(new BoxItem(box("a"), 1))
						.build();
				// <figure>
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
				// </figure>
				figure(result);

				PackagerResultAssert.assertThat(result).isSuccess();
				assertThat(result.getContainers()).extracting(Container::getId).containsExactly("large");
			} finally {
				packager.close();
			}
		}
	}

	@Test
	void largestContainerFirstStrategyContinuesWithTheNextLargestContainer() {
		//  large [a][b]   then   small [c]
		//
		for(AbstractPackager<?> packager : largestContainerFirstPackagers()) {
			try {
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container("small", 1), 2), new ContainerItem(container("large", 2), 1)))
						.withMaxContainerCount(2)
						.withBoxItems(new BoxItem(box("cube"), 3))
						.build();
				// <figure>
				// container 1 of 2: large
				//   z                         z                         y                         z
				//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
				//   | /-------/-------|   y     |       |       |         |       |       |         |       |
				//   |/       /       /|         | cube  | cube  |         | cube  | cube  |         | cube  |
				// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
				//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
				//   | cube  | cube  | | 1       0       1       2   x     0       1       2   x     0       1   y
				//   |       |       |/
				// 0 |-------|-------|-- x
				//   0       1       2
				//
				// container 2 of 2: small
				//   z                 z                 y                 z
				//                     1 +-------+       1 +-------+       1 +-------+
				//   | /-------|   y     |       |         |       |         |       |
				//   |/       /|         | cube  |         | cube  |         | cube  |
				// 1 |-------| | /       |       |         |       |         |       |
				//   |       | |/      0 +-------+       0 +-------+       0 +-------+
				//   | cube  | | 1       0       1   x     0       1   x     0       1   y
				//   |       |/
				// 0 |-------|-- x
				//   0       1
				// </figure>
				figure(result);

				PackagerResultAssert.assertThat(result).isSuccess();
				assertThat(result.getContainers()).extracting(Container::getId).containsExactly("large", "small");
				assertThat(result.getContainers()).extracting(c -> c.getStack().size()).containsExactly(2, 1);
			} finally {
				packager.close();
			}
		}
	}

	@Test
	void largestContainerFirstStrategyPacksBoxItemGroups() {
		//  large [first][second]
		//
		for(AbstractPackager<?> packager : largestContainerFirstPackagers()) {
			try {
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container("small", 1), 2), new ContainerItem(container("large", 2), 1)))
						.withMaxContainerCount(2)
						.withBoxItems(new BoxItemGroup("first", List.of(new BoxItem(box("a"), 1))), new BoxItemGroup("second", List.of(new BoxItem(box("b"), 1))))
						.build();
				// <figure>
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
				// </figure>
				figure(result);

				PackagerResultAssert.assertThat(result).isSuccess();
				assertThat(result.getContainers()).extracting(Container::getId).containsExactly("large");
			} finally {
				packager.close();
			}
		}
	}

	@Test
	void largestContainerFirstStrategyFailsWhenTheContainersAreUsedUp() {
		//  large [a][b]   c does not fit within the max container count
		//
		for(AbstractPackager<?> packager : largestContainerFirstPackagers()) {
			try {
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container("small", 1), 2), new ContainerItem(container("large", 2), 1)))
						.withMaxContainerCount(1)
						.withBoxItems(new BoxItem(box("cube"), 3))
						.build();

				assertThat(result.isSuccess()).isFalse();
			} finally {
				packager.close();
			}
		}
	}

	@Test
	void backToFrontPlacementComparatorFillsTheBackWallFirst() {
		// Container 2x1x2 seen from the side, the back wall at x = 0. The default placement
		// comparator prefers lower z, so it would place b on the floor next to a.
		//
		//  z
		//  1  [b][ ]
		//  0  [a][ ]
		//      0  1  x
		//
		PlainPackager packager = PlainPackager.newBuilder()
				.withPlacementControlsBuilderFactory(b -> b.withPlacementComparator(new BackToFrontPlacementComparator()))
				.build();
		try {
			Container container = Container.newBuilder().withId("container").withSize(2, 1, 2).withMaxLoadWeight(2).build();
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 1)))
					.withBoxItems(new BoxItem(box("cube"), 2))
					.build();
			// <figure>
			//   z                 z                 y                 z
			//                     2 +-------+       1 +-------+       2 +-------+
			//   | /-------|         |       |         |       |         |       |
			//   |/       /|         | cube  |         | cube  |         | cube  |
			// 2 |-------| |         |       |         |       |         |       |
			//   |       | |       1 +-------+       0 +-------+       1 +-------+
			//   | cube  | |   y     |       |         0       1   x     |       |
			//   |       |/|         | cube  |                           | cube  |
			// 1 |-------| | /       |       |                           |       |
			//   |       | |/      0 +-------+                         0 +-------+
			//   | cube  | | 1       0       1   x                       0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			figure(result);

			PackagerResultAssert.assertThat(result).isSuccess();
			List<Placement> placements = result.getContainers().get(0).getStack().getPlacements();
			assertThat(placements).extracting(Placement::getAbsoluteX).containsExactly(0, 0);
			assertThat(placements).extracting(Placement::getAbsoluteZ).containsExactlyInAnyOrder(0, 1);
		} finally {
			packager.close();
		}
	}

	private static List<AbstractPackager<?>> largestContainerFirstPackagers() {
		return List.of(
				PlainPackager.newBuilder().withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				LargestAreaFitFirstPackager.newBuilder().withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				BruteForcePackager.newBuilder().withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				FastBruteForcePackager.newBuilder().withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				FastLargestAreaFitFirstPackager.newBuilder().withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2)
						.withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy()).build(),
				CompositePackager.newBuilder()
						.withPackager(PlainPackager.newBuilder().build())
						.withPackager(FastBruteForcePackager.newBuilder().build(), 1000)
						.withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy())
						.build());
	}

	private static Box box(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private static Container container(String id, int dx) {
		return Container.newBuilder().withId(id).withSize(dx, 1, 1).withMaxLoadWeight(dx).build();
	}
}
