package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.ascii.ContainerAsciiArt;
import com.github.skjolber.packing.test.ascii.Figure;
import com.github.skjolber.packing.test.ascii.FigureRecorder;
import com.github.skjolber.packing.test.ascii.Figures;
import com.github.skjolber.packing.test.ascii.Style;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.test.example.ConstrainedPlacementControlsBuilderFactory;
import com.github.skjolber.packing.test.example.ForbiddenRegion;
import com.github.skjolber.packing.test.example.PlacementConstraint;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * Worked example: a custom placement controls system with
 * <ul>
 * <li>full support as a setting for the whole packager,
 * <li>a placement constraint for each box ({@linkplain PlacementConstraint#SAME_TYPE_STACK} and {@linkplain PlacementConstraint#GROUND_ONLY}), and
 * <li>a region where no box may be placed (an obstacle).
 * </ul>
 * The controls, their builder and the factory only depend on the api module, and are in the test module, package
 * {@code com.github.skjolber.packing.test.example} (as the other extension examples are, see {@linkplain ApiExtensionTest}). This test
 * configures them on the plain packager, which is how DEVELOPER.md describes it: read the controls together with
 * "Writing your own placement controls" there.
 * <p>
 * Each rule has a test which packs an input where the rule matters, next to the same input without the rule, and checks the rule on
 * the placements (not just that the packing succeeded). Another test combines the rules. The boxes are listed in the order they are
 * to be placed, as the example controls place the first box that fits.
 */
class ConstrainedPlacementControlsExampleTest {

	/**
	 * Container 2 x 1 x 2. The wide box is twice as wide as the small box. When the small box is placed first, the only place the wide
	 * box fits is on top of it, where it hangs over the floor next to it. With full support required, the wide box is placed on the
	 * floor of a container of its own instead.
	 */
	@Test
	void requireFullSupportKeepsABoxFromOverhanging() {
		Container container = container(2, 1, 2);
		Box small = cube("small");
		Box wide = Box.newBuilder().withId("wide").withSize(2, 1, 1).withWeight(1).build();

		PackagerResult overhanging = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(small, 1), new BoxItem(wide, 1));
		// without the requirement, the wide box rests on the small box, and hangs over the floor
		// <figure>
		//   z                         z                         y                         z
		//                             2 +---------------+       1 +---------------+       2 +-------+
		//   | /---------------|         |               |         |               |         |       |
		//   |/               /|         |     wide      |         |     wide      |         | wide  |
		// 2 |---------------| |         |               |         |               |         |       |
		//   |               | |       1 +-------+-------+       0 +---------------+       1 +-------+
		//   |     wide      | |   y     |       |                 0       1       2   x     |       |
		//   |               |/          | small |                                           | small |
		// 1 |-------|-------|   /       |       |                                           |       |
		//   |       | |        /      0 +-------+                                         0 +-------+
		//   | small | |       / 1       0       1       2   x                               0       1   y
		//   |       |/       /
		// 0 |-------|---------- x
		//   0       1       2
		// </figure>
		figure(overhanging);

		PackagerResultAssert.assertThat(overhanging).isSuccess().hasContainerCount(1);
		assertThat(placement(overhanging, "wide").getAbsoluteZ()).isEqualTo(1);
		assertThat(isFullySupported(placement(overhanging, "wide"), overhanging.getContainers().get(0).getStack().getPlacements())).isFalse();

		PackagerResult supported = pack(new ConstrainedPlacementControlsBuilderFactory().withRequireFullSupport(true), container, 2, new BoxItem(small, 1), new BoxItem(wide, 1));
		// with the requirement, the wide box is on the floor of a container of its own
		// <figure>
		// container 1 of 2: container
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | small |         | small |         | small |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | small | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		//
		// container 2 of 2: container
		//   z                         z                         y                         z
		//                             1 +---------------+       1 +---------------+       1 +-------+
		//   | /---------------|   y     |               |         |               |         |       |
		//   |/               /|         |     wide      |         |     wide      |         | wide  |
		// 1 |---------------| | /       |               |         |               |         |       |
		//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
		//   |     wide      | | 1       0               2   x     0               2   x     0       1   y
		//   |               |/
		// 0 |---------------|-- x
		//   0               2
		// </figure>
		figure(supported);

		// the wide box cannot rest on the small box alone, so it is on the floor of a container of its own
		PackagerResultAssert.assertThat(supported).isSuccess().hasContainerCount(2);
		assertFullySupported(supported);
		assertThat(placement(supported, "wide").getAbsoluteZ()).isEqualTo(0);
	}

	/**
	 * Container 1 x 1 x 2, a tower for two cubes. The free box is placed first. The ground box is stacked on it, unless it is marked
	 * ground only: then it is placed on the floor of a container of its own.
	 */
	@Test
	void groundOnlyBoxIsPlacedOnTheFloor() {
		Container container = container(1, 1, 2);
		Box free = cube("free");

		PackagerResult stacked = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(free, 1), new BoxItem(cube("ground"), 1));
		// without the constraint, the ground box is stacked on the free box
		// <figure>
		//   z                  z                  y                  z
		//                      2 +--------+       1 +--------+       2 +--------+
		//   | /--------|         |        |         |        |         |        |
		//   |/        /|         | ground |         | ground |         | ground |
		// 2 |--------| |         |        |         |        |         |        |
		//   |        | |       1 +--------+       0 +--------+       1 +--------+
		//   | ground | |   y     |        |         0        1   x     |        |
		//   |        |/|         |  free  |                            |  free  |
		// 1 |--------| | /       |        |                            |        |
		//   |        | |/      0 +--------+                          0 +--------+
		//   |  free  | | 1       0        1   x                        0        1   y
		//   |        |/
		// 0 |--------|-- x
		//   0        1
		// </figure>
		figure(stacked);

		PackagerResultAssert.assertThat(stacked).isSuccess().hasContainerCount(1);
		assertThat(placement(stacked, "ground").getAbsoluteZ()).isEqualTo(1);

		Box ground = cube("ground", PlacementConstraint.GROUND_ONLY);
		PackagerResult onTheFloor = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(free, 1), new BoxItem(ground, 1));
		// ground only: the ground box is on the floor of a container of its own
		// <figure>
		// container 1 of 2: container
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | free  |         | free  |         | free  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | free  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		//
		// container 2 of 2: container
		//   z                  z                  y                  z
		//                      1 +--------+       1 +--------+       1 +--------+
		//   | /--------|   y     |        |         |        |         |        |
		//   |/        /|         | ground |         | ground |         | ground |
		// 1 |--------| | /       |        |         |        |         |        |
		//   |        | |/      0 +--------+       0 +--------+       0 +--------+
		//   | ground | | 1       0        1   x     0        1   x     0        1   y
		//   |        |/
		// 0 |--------|-- x
		//   0        1
		// </figure>
		figure(onTheFloor);

		PackagerResultAssert.assertThat(onTheFloor).isSuccess().hasContainerCount(2);
		assertGroundOnlyBoxesAreOnTheFloor(onTheFloor);
		assertThat(placement(onTheFloor, "ground").getAbsoluteZ()).isEqualTo(0);
	}

	/**
	 * Container 1 x 1 x 3, a tower for three cubes: one box b, and two boxes a. The box b is placed first. The boxes a are stacked on
	 * it, unless they are marked same-type stack: then they are placed in a container of their own, where one rests on the other, as
	 * they are of the same type.
	 */
	@Test
	void sameTypeStackBoxRestsOnlyOnTheSameType() {
		Container container = container(1, 1, 3);
		Box b = cube("b");

		PackagerResult mixed = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(b, 1), new BoxItem(cube("a"), 2));
		// without the constraint, the boxes a are stacked on the box b
		// <figure>
		//   z                 z                 y                 z
		//                     3 +-------+       1 +-------+       3 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         |   a   |         |   a   |         |   a   |
		// 3 |-------| |         |       |         |       |         |       |
		//   |       | |       2 +-------+       0 +-------+       2 +-------+
		//   |   a   | |         |       |         0       1   x     |       |
		//   |       |/|         |   a   |                           |   a   |
		// 2 |-------| |         |       |                           |       |
		//   |       | |       1 +-------+                         1 +-------+
		//   |   a   | |   y     |       |                           |       |
		//   |       |/|         |   b   |                           |   b   |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   |   b   | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(mixed);

		PackagerResultAssert.assertThat(mixed).isSuccess().hasContainerCount(1);

		Box a = cube("a", PlacementConstraint.SAME_TYPE_STACK);
		PackagerResult sameType = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(b, 1), new BoxItem(a, 2));
		// same-type stack: the boxes a are not stacked on the box b, but on each other
		// <figure>
		// container 1 of 2: container
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
		//
		// container 2 of 2: container
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         |   a   |         |   a   |         |   a   |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   |   a   | |   y     |       |         0       1   x     |       |
		//   |       |/|         |   a   |                           |   a   |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   |   a   | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(sameType);

		// the boxes a are stacked on each other (the same type), but not on the box b
		PackagerResultAssert.assertThat(sameType).isSuccess().hasContainerCount(2);
		assertSameTypeStackBoxesRestOnTheSameType(sameType);
		assertThat(sameType.getContainers().get(1).getStack().getPlacements()).extracting(p -> p.getBox().getId()).containsExactly("a", "a");
	}

	/**
	 * Container 2 x 2 x 1 for four cubes. The region (#) takes up the cell x = 1, y = 1, so the fourth cube does not fit in the first
	 * container, and is placed in the second (the region is the same in every container). The packager does not know about the region:
	 * it is drawn into the figure only.
	 */
	@Test
	void forbiddenRegionIsLeftEmpty() {
		Container container = container(2, 2, 1);
		Box cube = cube("c");
		ForbiddenRegion region = new ForbiddenRegion(1, 1, 0, 1, 1, 1);

		PackagerResult withoutRegion = pack(new ConstrainedPlacementControlsBuilderFactory(), container, 2, new BoxItem(cube, 4));
		PackagerResultAssert.assertThat(withoutRegion).isSuccess().hasContainerCount(1);

		PackagerResult result = pack(new ConstrainedPlacementControlsBuilderFactory().withForbiddenRegion(1, 1, 0, 1, 1, 1), container, 2, new BoxItem(cube, 4));
		// the region (#) is drawn by the test, for the figure only: the controls veto the placements which intersect it
		// <figure>
		// container 1 of 2
		//   z   /-------/-------|   y   z                         y                         z
		//      /   c   /#######/|       1 +-------+-------+       2 +-------+-------+       1 +-------+-------+
		//   | /-------/-------|#| /       |       |       |         |       |#######|         |       |#######|
		//   |/       /       /|#|/        |   c   |   c   |         |   c   |#######|         |   c   |#######|
		// 1 |-------|-------| |#| 2       |       |       |         |       |#######|         |       |#######|
		//   |       |       | |/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
		//   |   c   |   c   | | 1         0       1       2   x     |       |       |         0       1       2   y
		//   |       |       |/                                      |   c   |   c   |
		// 0 |-------|-------|-- x                                   |       |       |
		//   0       1       2                                     0 +-------+-------+
		//                                                           0       1       2   x
		//
		// container 2 of 2
		//   z           /-------|   y   z                         y                         z
		//              /#######/|       1 +-------+-------+       2         +-------+       1 +-------+-------+
		//   | /-------|-------|#| /       |       |#######|                 |#######|         |       |#######|
		//   |/       /|#######|#|/        |   c   |#######|                 |#######|         |   c   |#######|
		// 1 |-------| |#######|#| 2       |       |#######|                 |#######|         |       |#######|
		//   |       | |#######|/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
		//   |   c   | |-------| 1         0       1       2   x     |       |                 0       1       2   y
		//   |       |/       /                                      |   c   |
		// 0 |-------|---------- x                                   |       |
		//   0       1       2                                     0 +-------+
		//                                                           0       1       2   x
		// </figure>
		figureWithRegions(result, region);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(2);
		assertRegionsAreEmpty(result, region);
		assertThat(result.getContainers()).extracting(c -> c.getStack().size()).containsExactly(3, 1);
	}

	/**
	 * Container 2 x 2 x 2 with the region (#) on the floor at x = 1, y = 1, and all the rules at once. The boxes are placed in the
	 * order they are listed in: the boxes a (stacked on their own type only), the ground boxes g, then the wide box, which has no
	 * constraint of its own. Full support is required, so the wide box must rest on boxes under its whole length.
	 * <p>
	 * The fourth box a is stacked on another box a, as the floor is full, whereas the fourth box g cannot be stacked and goes to the
	 * second container, where the wide box rests on two of the other boxes g.
	 */
	@Test
	void theRulesCanBeCombined() throws Exception {
		Container container = container(2, 2, 2);
		ForbiddenRegion region = new ForbiddenRegion(1, 1, 0, 1, 1, 1);
		Box g = cube("g", PlacementConstraint.GROUND_ONLY);
		Box a = cube("a", PlacementConstraint.SAME_TYPE_STACK);
		Box wide = Box.newBuilder().withId("wide").withSize(2, 1, 1).withWeight(1).build();
		List<BoxItem> boxItems = List.of(new BoxItem(a, 4), new BoxItem(g, 4), new BoxItem(wide, 1));

		ConstrainedPlacementControlsBuilderFactory factory = new ConstrainedPlacementControlsBuilderFactory()
				.withRequireFullSupport(true)
				.withForbiddenRegion(1, 1, 0, 1, 1, 1);
		PackagerResult result = pack(factory, container, 2, boxItems.toArray(new BoxItem[0]));
		// <figure>
		// container 1 of 2
		//   z   /-------|               z                         y                         z
		//      /   a   /|               2 +-------+               2 +-------+-------+       2 +-------+-------+
		//   | /-------| |                 |       |                 |       |#######|         |       |       |
		//   |/       /| |                 |   a   |                 |   a   |#######|         |   a   |   a   |
		// 2 |-------| | |-------|   y     |       |                 |       |#######|         |       |       |
		//   |       | |/#######/|       1 +-------+-------+       1 +-------+-------+       1 +-------+-------+
		//   |   a   | |-------|#| /       |       |       |         |       |       |         |       |#######|
		//   |       |/       /|#|/        |   a   |   g   |         |   a   |   g   |         |   g   |#######|
		// 1 |-------|-------| |#| 2       |       |       |         |       |       |         |       |#######|
		//   |       |       | |/        0 +-------+-------+       0 +-------+-------+       0 +-------+-------+
		//   |   a   |   g   | | 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		//
		// container 2 of 2
		//   z                           z                         y                         z
		//                               2 +---------------+       2 +-------+-------+       2 +-------+
		//   | /---------------|           |               |         |       |#######|         |       |
		//   |/               /|           |     wide      |         |   g   |#######|         | wide  |
		// 2 |---------------| |-|   y     |               |         |       |#######|         |       |
		//   |               | |/|       1 +-------+-------+       1 +-------+-------+       1 +-------+-------+
		//   |     wide      | |#| /       |       |       |         |               |         |       |#######|
		//   |               |/|#|/        |   g   |   g   |         |     wide      |         |   g   |#######|
		// 1 |-------|-------| |#| 2       |       |       |         |               |         |       |#######|
		//   |       |       | |/        0 +-------+-------+       0 +---------------+       0 +-------+-------+
		//   |   g   |   g   | | 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figureWithRegions(result, region);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(2);
		assertGroundOnlyBoxesAreOnTheFloor(result);
		assertSameTypeStackBoxesRestOnTheSameType(result);
		assertRegionsAreEmpty(result, region);
		assertFullySupported(result);
		// the layout is valid for the packager's own rules too
		try (DefaultValidator validator = new DefaultValidator()) {
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 2)))
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	/**
	 * Replacing the default placement controls does not retain their checks, so the builder of the example controls rejects what
	 * the controls do not implement, as DEVELOPER.md advises.
	 */
	@Test
	void rejectsWhatTheControlsDoNotImplement() {
		ConstrainedPlacementControlsBuilderFactory factory = new ConstrainedPlacementControlsBuilderFactory();
		Container container = container(2, 2, 1);
		BoxItem boxItem = new BoxItem(cube("c"), 1);

		try (PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory(factory).build()) {
			assertThatThrownBy(() -> packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 1)))
					.withBoxItems(boxItem)
					.withOrder(Order.CHRONOLOGICAL)
					.build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("box item order");

			// a box which is already in the container
			Placement obstacle = new Placement(cube("obstacle").getStackValue(0), -1, 0, 0, 0);
			Container withObstacle = container.withObstacles(List.of(obstacle));
			assertThatThrownBy(() -> packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(withObstacle, 1)))
					.withBoxItems(boxItem)
					.build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("obstacles");
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// assertions on the placements, independent of the controls
	// ------------------------------------------------------------------------------------------------------------------------------

	/** A box with a ground-only constraint never has a box below it: it is at z = 0. */
	private static void assertGroundOnlyBoxesAreOnTheFloor(PackagerResult result) {
		int checked = 0;
		for(Placement placement : placements(result)) {
			if(PlacementConstraint.of(placement.getBoxItem()) == PlacementConstraint.GROUND_ONLY) {
				assertThat(placement.getAbsoluteZ()).as("ground-only box %s", placement.getBox().getId()).isZero();
				checked++;
			}
		}
		assertThat(checked).as("ground-only boxes in the result").isPositive();
	}

	/** A box with a same-type constraint which is not on the floor rests on boxes with its id only, and on at least one. */
	private static void assertSameTypeStackBoxesRestOnTheSameType(PackagerResult result) {
		int checked = 0;
		for(Container container : result.getContainers()) {
			List<Placement> placements = container.getStack().getPlacements();
			for(Placement placement : placements) {
				if(PlacementConstraint.of(placement.getBoxItem()) != PlacementConstraint.SAME_TYPE_STACK || placement.getAbsoluteZ() == 0) {
					continue;
				}
				List<Placement> below = placementsBelow(placement, placements);
				assertThat(below).as("boxes below %s", placement).isNotEmpty();
				assertThat(below).as("boxes below %s", placement).allMatch(p -> p.getBox().getId().equals(placement.getBox().getId()));
				checked++;
			}
		}
		assertThat(checked).as("stacked same-type boxes in the result").isPositive();
	}

	/** No box takes up any space in any of the regions. */
	private static void assertRegionsAreEmpty(PackagerResult result, ForbiddenRegion... regions) {
		for(ForbiddenRegion region : regions) {
			Placement regionPlacement = regionPlacement(region);
			for(Placement placement : placements(result)) {
				assertThat(placement.intersects3D(regionPlacement)).as("%s in %s", placement, region).isFalse();
			}
		}
	}

	/** Every box above the floor has its whole bottom face on the boxes below it. */
	private static void assertFullySupported(PackagerResult result) {
		for(Container container : result.getContainers()) {
			List<Placement> placements = container.getStack().getPlacements();
			for(Placement placement : placements) {
				assertThat(isFullySupported(placement, placements)).as("support of %s", placement).isTrue();
			}
		}
	}

	private static boolean isFullySupported(Placement placement, List<Placement> placements) {
		if(placement.getAbsoluteZ() == 0) {
			return true;
		}
		long supportedArea = 0;
		for(Placement below : placementsBelow(placement, placements)) {
			supportedArea += below.overlapArea2D(placement);
		}
		return supportedArea == placement.getStackValue().getArea();
	}

	/** The placements which end right under the placement, and share some of its bottom face. */
	private static List<Placement> placementsBelow(Placement placement, List<Placement> placements) {
		List<Placement> below = new ArrayList<>();
		for(Placement candidate : placements) {
			if(candidate.getAbsoluteEndZ() == placement.getAbsoluteZ() - 1 && candidate.overlapArea2D(placement) > 0) {
				below.add(candidate);
			}
		}
		return below;
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// figures
	// ------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Like {@code PackagerResultFigures.figure(result)}, with the regions drawn as obstacles (#), which they are not for the packager:
	 * they are only drawn into the figure.
	 */
	private static void figureWithRegions(PackagerResult result, ForbiddenRegion... regions) {
		if(!FigureRecorder.isEnabled()) {
			return;
		}
		List<Placement> obstacles = new ArrayList<>();
		for(ForbiddenRegion region : regions) {
			obstacles.add(regionPlacement(region));
		}
		List<Figure> figures = new ArrayList<>();
		List<Container> containers = result.getContainers();
		for(int i = 0; i < containers.size(); i++) {
			Container container = containers.get(i).withObstacles(obstacles);
			Figure overview = ContainerAsciiArt.newBuilder().withContainer(container).withStyle(Style.ASCII).build().overview();
			if(containers.size() > 1) {
				overview = Figures.vertical(0, Figure.of("container " + (i + 1) + " of " + containers.size()), overview);
			}
			figures.add(overview);
		}
		FigureRecorder.record(Figures.vertical(1, figures.toArray(new Figure[0])));
	}

	private static Placement regionPlacement(ForbiddenRegion region) {
		Box box = Box.newBuilder().withSize(region.dx(), region.dy(), region.dz()).withWeight(0).build();
		return new Placement(box.getStackValue(0), -1, region.x(), region.y(), region.z());
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// input
	// ------------------------------------------------------------------------------------------------------------------------------

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withId("container").withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}

	private static Box cube(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private static Box cube(String id, PlacementConstraint constraint) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).withProperty(PlacementConstraint.PROPERTY, constraint).build();
	}

	private static PackagerResult pack(ConstrainedPlacementControlsBuilderFactory factory, Container container, int maxContainerCount, BoxItem... boxItems) {
		try (PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory(factory).build()) {
			return packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, maxContainerCount)))
					.withMaxContainerCount(maxContainerCount)
					.withBoxItems(boxItems)
					.withInterruptDuration(10_000)
					.build();
		}
	}

	private static List<Placement> placements(PackagerResult result) {
		List<Placement> placements = new ArrayList<>();
		for(Container container : result.getContainers()) {
			placements.addAll(container.getStack().getPlacements());
		}
		return placements;
	}

	/** The (first) placement of a box, with its id */
	private static Placement placement(PackagerResult result, String boxId) {
		for(Placement placement : placements(result)) {
			if(boxId.equals(placement.getBox().getId())) {
				return placement;
			}
		}
		throw new AssertionError("No placement of box " + boxId);
	}
}
