package com.github.skjolber.packing.packer.laff;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;

/**
 * A box which fits neither the current level nor a new level on top of it: the level is raised to the top of the
 * container, so that the box stands beside the level's boxes.
 */
public class RaisedLevelTest extends AbstractPackagerTest {

	static Stream<Arguments> packagers() {
		return Stream.of(
				Arguments.of("laff", (Supplier<AbstractPackager<?>>) () -> LargestAreaFitFirstPackager.newBuilder().build()),
				Arguments.of("fastLaff", (Supplier<AbstractPackager<?>>) () -> FastLargestAreaFitFirstPackager.newBuilder().build()));
	}

	private static List<ContainerItem> containerItems() {
		return ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(6, 4, 3).withMaxLoadWeight(100).build(), 1)
				.build();
	}

	/**
	 * A (4x4x2) starts the first level, which is 2 high. B (2x4x3) is too tall for the level, and for a new level on
	 * top (1 high), so it stands beside A.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("packagers")
	void tallBoxStandsBesideTheLevel(String name, Supplier<AbstractPackager<?>> supplier) {
		List<ContainerItem> containerItems = containerItems();
		List<BoxItem> products = Arrays.asList(
				new BoxItem(Box.newBuilder().withId("A").withRotate2D().withSize(4, 4, 2).withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("B").withRotate2D().withSize(2, 4, 3).withWeight(1).build(), 1));

		try (AbstractPackager<?> packager = supplier.get()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).withMaxContainerCount(1).build();
			// <figure>
			//   z                   /-------|       z                                 y                                 z
			//                      /       /|       3                 +-------+       4 +---------------+-------+       3 +---------------+
			//   |   /-------------/       / |   y                     |       |         |               |       |         |               |
			//   |  /             /       /  |       2 +---------------+       |         |               |       |       2 |               |
			// 3 | /             |-------|   | /       |               |   B   |         |               |       |         |       B       |
			//   |/              |       |   |/        |       A       |       |         |       A       |   B   |         |               |
			// 2 |---------------|       |   | 4       |               |       |         |               |       |         |               |
			//   |               |   B   |  /        0 +---------------+-------+         |               |       |       0 +---------------+
			//   |       A       |       | /           0               4       6   x     |               |       |         0               4   y
			//   |               |       |/                                            0 +---------------+-------+
			// 0 |---------------|-------|-- x                                           0               4       6   x
			//   0               4       6
			// </figure>
			figure(result);

			PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(1).hasStackSize(0, 2);
			List<Placement> placements = result.get(0).getStack().getPlacements();
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(4, 0, 0).isEndAt(5, 3, 2).hasBoxItemId("B");

			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		}
	}

	/**
	 * A group of A (2x2x1), which carries at most 1, and B (1x1x2), which weighs 3. A starts the first level, which is
	 * 1 high. B is too tall for the level, and A cannot carry it in a new level on top, so B stands beside A.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("packagers")
	void groupBoxWhichTheLevelCannotCarryStandsBesideIt(String name, Supplier<AbstractPackager<?>> supplier) {
		List<ContainerItem> containerItems = containerItems();
		List<BoxItemGroup> groups = Arrays.asList(new BoxItemGroup("group", Arrays.asList(
				new BoxItem(Box.newBuilder().withId("A").withRotate2D().withSize(2, 2, 1).withWeight(2).withMaxLoadWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("B").withRotate2D().withSize(1, 1, 2).withWeight(3).build(), 1))));

		try (AbstractPackager<?> packager = supplier.get()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(groups).withMaxContainerCount(1).build();
			// <figure>
			//         /-------|               z                         y                         z
			//        /   B   /|               2 +-------+               3 +-------+               2                 +-------+
			//   z   |-------| |                 |       |                 |       |                                 |       |
			//       |       | |                 |   B   |                 |   B   |                                 |       |
			//   |   |       | |           y     |       |                 |       |                                 |       |
			//   |   |       | |               1 +-------+-------+       2 +-------+-------+       1 +---------------+   B   |
			// 2 |   |-------|-------|   /       |               |         |               |         |               |       |
			//   |  /               /|  /        |       A       |         |               |         |       A       |       |
			//   | /               / | / 3       |               |         |               |         |               |       |
			//   |/               /  |/        0 +---------------+         |       A       |       0 +---------------+-------+
			// 1 |---------------|   | 2         0       1       2   x     |               |         0               2       3   y
			//   |               |  /                                      |               |
			//   |       A       | /                                       |               |
			//   |               |/                                      0 +---------------+
			// 0 |---------------|-- x                                     0       1       2   x
			//   0               2
			// </figure>
			figure(result);

			PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(1).hasStackSize(0, 2);
			List<Placement> placements = result.get(0).getStack().getPlacements();
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAtZ(0).hasBoxItemId("B");

			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItemGroups(groups));
			Container container = result.get(0);
			PackagerResultAssert.assertThat(result).isAcceptedBy(new DefaultLoadValidatorBuilder()
					.withPlacements(container.getStack().getPlacements())
					.withContainer(container)
					.build());
		}
	}
}
