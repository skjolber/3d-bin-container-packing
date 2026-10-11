package com.github.skjolber.packing.packer.example;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsOnGroundLevelPointControls;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;

/**
 * Worked example: placement rules which look like they need custom placement controls, but do not, as the library has them. Custom
 * code is only for what the library cannot do, so the only custom code used is the point controls of the heavy items example
 * ({@linkplain HeavyItemsOnGroundLevelPointControls}, in the test sources of this module).
 * <ul>
 * <li>Full support is an option of the packager: {@code withRequireFullSupport(true)}.
 * <li>Keeping boxes out of a part of the container is done with the obstacles of the container item.
 * <li>A box which carries boxes of its own type only is a load constraint of the box: {@code withMaxLoadIdenticalBoxCount(-1)} (no
 * limit on the number of boxes on top, but they must be identical).
 * <li>A box which must be on the floor is a rule for which points are available to the box: point controls of the container item. The
 * point controls of this example place the boxes which are heavier than a limit on the floor.
 * </ul>
 * Custom placement controls replace the default ones, and so do not retain such rules (see "Writing your own placement controls" in
 * DEVELOPER.md): configure the packager instead, as here.
 * <p>
 * Each rule has a test which packs an input where the rule matters, next to the same input without the rule, and checks the rule on
 * the placements (not just that the packing succeeded). Another test combines the rules. The boxes are ranked as the plain packager does
 * by default: the box with the largest volume (then the largest weight) is placed first, and boxes which are alike in that are placed in
 * the order they are listed in.
 */
class PlacementRulesExampleTest {

	/** Boxes which are heavier than this are placed on the floor only. */
	private static final int MAX_WEIGHT_OFF_THE_FLOOR = 3;

	/**
	 * Container 2 x 1 x 3. The post is placed first (it has the same volume as the plank, and is heavier). The plank is twice as wide as
	 * the post, so the only place it fits is on top of the post, where it hangs over the floor next to it. With full support required,
	 * the plank is placed on the floor of a container of its own instead.
	 */
	@Test
	void requireFullSupportKeepsABoxFromOverhanging() {
		Container container = container(2, 1, 3);
		BoxItem post = new BoxItem(box("post", 1, 1, 2, 2), 1);
		BoxItem plank = new BoxItem(box("plank", 2, 1, 1, 1), 1);

		PackagerResult overhanging = pack(false, null, container, 2, null, post, plank);
		// without the requirement, the plank rests on the post, and hangs over the floor
		// <figure>
		//   z                         z                         y                         z
		//                             3 +---------------+       1 +---------------+       3 +-------+
		//   | /---------------|         |               |         |               |         |       |
		//   |/               /|         |     plank     |         |     plank     |         | plank |
		// 3 |---------------| |         |               |         |               |         |       |
		//   |               | |       2 +-------+-------+       0 +---------------+       2 +-------+
		//   |     plank     | |         |       |                 0       1       2   x     |       |
		//   |               |/          |       |                                           |       |
		// 2 |-------|-------|           |       |                                           |       |
		//   |       | |                 | post  |                                           | post  |
		//   |       | |           y     |       |                                           |       |
		//   |       | |                 |       |                                           |       |
		//   | post  | |         /       |       |                                           |       |
		//   |       | |        /      0 +-------+                                         0 +-------+
		//   |       | |       / 1       0       1       2   x                               0       1   y
		//   |       |/       /
		// 0 |-------|---------- x
		//   0       1       2
		// </figure>
		figure(overhanging);

		PackagerResultAssert.assertThat(overhanging).isSuccess().hasContainerCount(1);
		assertThat(placement(overhanging, "plank").getAbsoluteZ()).isEqualTo(2);
		assertThat(isFullySupported(overhanging.getContainers().get(0).getStack().getPlacements())).isFalse();

		PackagerResult supported = pack(true, null, container, 2, null, post, plank);
		// with the requirement, the plank cannot rest on the post alone, so it is on the floor of a container of its own
		// <figure>
		// container 1 of 2: container
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         |       |         | post  |         |       |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |         | post  |       0 +-------+         | post  |
		//   |       | |   y     |       |         0       1   x     |       |
		//   |       | |         |       |                           |       |
		//   | post  | | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   |       | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		//
		// container 2 of 2: container
		//   z                         z                         y                         z
		//                             1 +---------------+       1 +---------------+       1 +-------+
		//   | /---------------|   y     |               |         |               |         |       |
		//   |/               /|         |     plank     |         |     plank     |         | plank |
		// 1 |---------------| | /       |               |         |               |         |       |
		//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
		//   |     plank     | | 1       0               2   x     0               2   x     0       1   y
		//   |               |/
		// 0 |---------------|-- x
		//   0               2
		// </figure>
		figure(supported);

		PackagerResultAssert.assertThat(supported).isSuccess().hasContainerCount(2);
		assertFullySupported(supported);
		assertThat(placement(supported, "post").getAbsoluteZ()).isEqualTo(0);
		assertThat(placement(supported, "plank").getAbsoluteZ()).isEqualTo(0);
		assertThat(supported.getContainers()).extracting(c -> c.getStack().size()).containsExactly(1, 1);
	}

	/**
	 * Container 1 x 1 x 3. The post is placed first (it has the largest volume), then the heavy cube. The cube is stacked on the post,
	 * unless the point controls of the container item only give it the points on the floor: then it is placed on the floor of a
	 * container of its own.
	 */
	@Test
	void groundOnlyBoxIsPlacedOnTheFloor() {
		Container container = container(1, 1, 3);
		BoxItem post = new BoxItem(box("post", 1, 1, 2, 1), 1);
		BoxItem heavy = new BoxItem(cube("heavy", MAX_WEIGHT_OFF_THE_FLOOR + 1), 1);

		PackagerResult stacked = pack(false, null, container, 2, null, post, heavy);
		// without the point controls, the heavy cube is stacked on the post
		// <figure>
		//   z                 z                 y                 z
		//                     3 +-------+       1 +-------+       3 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 3 |-------| |         |       |         |       |         |       |
		//   |       | |       2 +-------+       0 +-------+       2 +-------+
		//   | heavy | |         |       |         0       1   x     |       |
		//   |       |/|         |       |                           |       |
		// 2 |-------| |         |       |                           |       |
		//   |       | |         | post  |                           | post  |
		//   |       | |   y     |       |                           |       |
		//   |       | |         |       |                           |       |
		//   | post  | | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   |       | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(stacked);

		PackagerResultAssert.assertThat(stacked).isSuccess().hasContainerCount(1);
		assertThat(placement(stacked, "heavy").getAbsoluteZ()).isEqualTo(2);

		PackagerResult onTheFloor = pack(false, groundLevelForHeavyBoxes(), container, 2, null, post, heavy);
		// with the point controls, the heavy cube is on the floor of a container of its own
		// <figure>
		// container 1 of 2: container
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         |       |         | post  |         |       |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |         | post  |       0 +-------+         | post  |
		//   |       | |   y     |       |         0       1   x     |       |
		//   |       | |         |       |                           |       |
		//   | post  | | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   |       | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		//
		// container 2 of 2: container
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | heavy | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(onTheFloor);

		PackagerResultAssert.assertThat(onTheFloor).isSuccess().hasContainerCount(2);
		assertHeavyBoxesAreOnTheFloor(onTheFloor);
		assertThat(placement(onTheFloor, "heavy").getAbsoluteZ()).isEqualTo(0);
	}

	/**
	 * Container 1 x 1 x 3, a tower for three cubes: one box b, and two boxes a. The box b is placed first. The boxes a are stacked on
	 * it, unless it carries identical boxes only: then they are placed in a container of their own, where one rests on the other, as
	 * they are of the same type.
	 */
	@Test
	void identicalOnlyBoxCarriesOnlyTheSameType() {
		Container container = container(1, 1, 3);
		BoxItem a = new BoxItem(cube("a"), 2);

		PackagerResult mixed = pack(false, null, container, 2, null, new BoxItem(cube("b"), 1), a);
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

		Box b = Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).withMaxLoadIdenticalBoxCount(-1).build();
		PackagerResult identicalOnly = pack(false, null, container, 2, null, new BoxItem(b, 1), a);
		// the box b carries identical boxes only: the boxes a are not stacked on it, but on each other
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
		figure(identicalOnly);

		PackagerResultAssert.assertThat(identicalOnly).isSuccess().hasContainerCount(2);
		assertIdenticalOnlyBoxesCarryTheSameType(identicalOnly);
		assertThat(identicalOnly.getContainers().get(1).getStack().getPlacements()).extracting(p -> p.getBox().getId()).containsExactly("a", "a");
	}

	/**
	 * Container 2 x 2 x 1 for four cubes. The obstacle (#), which is given with the container item, takes up the cell x = 1, y = 1, so
	 * the fourth cube does not fit in the first container, and is placed in the second (the obstacle is in every container of the
	 * container item).
	 */
	@Test
	void obstaclesOfTheContainerAreLeftEmpty() {
		Container container = container(2, 2, 1);
		BoxItem cubes = new BoxItem(cube("c"), 4);

		PackagerResult withoutObstacle = pack(false, null, container, 2, null, cubes);
		PackagerResultAssert.assertThat(withoutObstacle).isSuccess().hasContainerCount(1);

		PackagerResult result = pack(false, null, container, 2, new int[] { 1, 1, 0, 1, 1, 1 }, cubes);
		// the obstacle is drawn as #
		// <figure>
		// container 1 of 2: container
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
		// container 2 of 2: container
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
		figure(result);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(2);
		assertObstaclesAreEmpty(result, 1);
		assertThat(result.getContainers()).extracting(c -> c.getStack().size()).containsExactly(3, 1);
	}

	/**
	 * Container 2 x 1 x 4 with an obstacle (#) on the floor at x = 1, and all the rules at once. What is left is a tower for four cubes
	 * at x = 0. The post is placed first, then the heavy cube, the box c and the free box f. The heavy cube may only be on the floor, so
	 * the box c is placed on top of the post. It carries identical boxes only, so the free box is not placed on it. The heavy cube and
	 * the free box are placed in a second container (where the obstacle is too), where the free box rests on the heavy cube, which
	 * carries any box. Full support is required, and holds.
	 */
	@Test
	void theRulesCanBeCombined() throws Exception {
		Container container = container(2, 1, 4);
		List<BoxItem> boxItems = List.of(
				new BoxItem(box("post", 1, 1, 2, 1), 1),
				new BoxItem(cube("heavy", MAX_WEIGHT_OFF_THE_FLOOR + 1), 1),
				new BoxItem(Box.newBuilder().withId("c").withSize(1, 1, 1).withWeight(MAX_WEIGHT_OFF_THE_FLOOR).withMaxLoadIdenticalBoxCount(-1).build(), 1),
				new BoxItem(cube("f"), 1));

		PackagerResult result = pack(true, groundLevelForHeavyBoxes(), container, 2, new int[] { 1, 0, 0, 1, 1, 1 }, boxItems.toArray(new BoxItem[0]));
		// <figure>
		// container 1 of 2: container
		//   z                         z                         y                         z
		//                             3 +-------+               1 +-------+-------+       3 +-------+
		//   | /-------|                 |       |                 |       |#######|         |       |
		//   |/       /|                 |   c   |                 |   c   |#######|         |   c   |
		// 3 |-------| |                 |       |                 |       |#######|         |       |
		//   |       | |               2 +-------+               0 +-------+-------+       2 +-------+
		//   |   c   | |                 |       |                 0       1       2   x     |       |
		//   |       |/|                 |       |                                           | post  |
		// 2 |-------| |                 |       |                                           |       |
		//   |       | |               1 | post  +-------+                                 1 +-------+
		//   |       | |-------|   y     |       |#######|                                   |#######|
		//   |       |/#######/|         |       |#######|                                   |#######|
		// 1 | post  |-------|#| /       |       |#######|                                   |#######|
		//   |       |#######|#|/      0 +-------+-------+                                 0 +-------+
		//   |       |#######|#| 1       0       1       2   x                               0       1   y
		//   |       |#######|/
		// 0 |-------|-------|-- x
		//   0       1       2
		//
		// container 2 of 2: container
		//   z                         z                         y                         z
		//                             2 +-------+               1 +-------+-------+       2 +-------+
		//   | /-------|                 |       |                 |       |#######|         |       |
		//   |/       /|                 |   f   |                 |   f   |#######|         |   f   |
		// 2 |-------| |                 |       |                 |       |#######|         |       |
		//   |       | |               1 +-------+-------+       0 +-------+-------+       1 +-------+
		//   |   f   | |-------|   y     |       |#######|         0       1       2   x     |#######|
		//   |       |/#######/|         | heavy |#######|                                   |#######|
		// 1 |-------|-------|#| /       |       |#######|                                   |#######|
		//   |       |#######|#|/      0 +-------+-------+                                 0 +-------+
		//   | heavy |#######|#| 1       0       1       2   x                               0       1   y
		//   |       |#######|/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(result);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(2);
		assertThat(result.getContainers().get(0).getStack().getPlacements()).extracting(p -> p.getBox().getId()).containsExactlyInAnyOrder("post", "c");
		assertThat(result.getContainers().get(1).getStack().getPlacements()).extracting(p -> p.getBox().getId()).containsExactlyInAnyOrder("heavy", "f");
		assertHeavyBoxesAreOnTheFloor(result);
		assertIdenticalOnlyBoxesCarryTheSameType(result);
		assertObstaclesAreEmpty(result, 1);
		assertFullySupported(result);
		// the layout is valid for the packager's own rules too
		try (DefaultValidator validator = new DefaultValidator()) {
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 2)))
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// assertions on the placements, independent of the packager
	// ------------------------------------------------------------------------------------------------------------------------------

	/** A box which is heavier than the limit never has a box below it: it is at z = 0. */
	private static void assertHeavyBoxesAreOnTheFloor(PackagerResult result) {
		int checked = 0;
		for(Placement placement : placements(result)) {
			if(placement.getBox().getWeight() > MAX_WEIGHT_OFF_THE_FLOOR) {
				assertThat(placement.getAbsoluteZ()).as("heavy box %s", placement.getBox().getId()).isZero();
				checked++;
			}
		}
		assertThat(checked).as("heavy boxes in the result").isPositive();
	}

	/** A box which carries identical boxes only has boxes with its id on top of it, or nothing. */
	private static void assertIdenticalOnlyBoxesCarryTheSameType(PackagerResult result) {
		int checked = 0;
		for(Container container : result.getContainers()) {
			List<Placement> placements = container.getStack().getPlacements();
			SupportGraph graph = new SupportGraph(placements);
			for(Placement placement : placements) {
				if(!placement.getBox().isLoadIdenticalBoxOnly()) {
					continue;
				}
				for(PlacementLoad supportee : graph.getSupportees(placement)) {
					assertThat(supportee.getPlacement().getBox().getId()).as("box on top of %s", placement).isEqualTo(placement.getBox().getId());
				}
				checked++;
			}
		}
		assertThat(checked).as("identical-only boxes in the result").isPositive();
	}

	/** Every container has the obstacles, and no box takes up any space in them. */
	private static void assertObstaclesAreEmpty(PackagerResult result, int obstacleCount) {
		for(Container container : result.getContainers()) {
			assertThat(container.getObstacles()).hasSize(obstacleCount);
			for(Placement obstacle : container.getObstacles()) {
				for(Placement placement : container.getStack().getPlacements()) {
					assertThat(placement.intersects3D(obstacle)).as("%s in %s", placement, obstacle).isFalse();
				}
			}
		}
	}

	/** Every box above the floor has its whole bottom face on the boxes below it. */
	private static void assertFullySupported(PackagerResult result) {
		for(Container container : result.getContainers()) {
			List<ValidatorResultReason> reasons = new ArrayList<>();
			assertThat(new FullySupportedStabilityValidator().isValid(container.getStack().getPlacements(), reasons)).as("full support: %s", reasons).isTrue();
		}
	}

	private static boolean isFullySupported(List<Placement> placements) {
		return new FullySupportedStabilityValidator().isValid(placements, new ArrayList<>());
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// input
	// ------------------------------------------------------------------------------------------------------------------------------

	private static PointControlsBuilderFactory groundLevelForHeavyBoxes() {
		return HeavyItemsOnGroundLevelPointControls.newFactory(MAX_WEIGHT_OFF_THE_FLOOR);
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withId("container").withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}

	private static Box box(String id, int dx, int dy, int dz, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).build();
	}

	private static Box cube(String id) {
		return cube(id, 1);
	}

	private static Box cube(String id, int weight) {
		return box(id, 1, 1, 1, weight);
	}

	/**
	 * @param requireFullSupport the option of the packager
	 * @param pointControls point controls of the container item, or null for none
	 * @param obstacle x, y, z, dx, dy and dz of an obstacle in every container, or null for none
	 */
	private static PackagerResult pack(boolean requireFullSupport, PointControlsBuilderFactory pointControls, Container container, int maxContainerCount, int[] obstacle,
			BoxItem... boxItems) {
		try (PlainPackager packager = PlainPackager.newBuilder().withRequireFullSupport(requireFullSupport).build()) {
			return packager.newResultBuilder()
					.withContainerItem(b -> {
						b.withContainerItem(container, maxContainerCount);
						if(pointControls != null) {
							b.withPointControlsBuilderFactory(pointControls);
						}
						if(obstacle != null) {
							b.withObstacles(o -> o.withObstacle(obstacle[0], obstacle[1], obstacle[2], obstacle[3], obstacle[4], obstacle[5]));
						}
					})
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
