package com.github.skjolber.packing.packer.example;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * Worked example: placement rules which look like they need custom placement controls, but do not, as the library has them. Custom
 * code is only for what the library cannot do, so the only custom code used is the point controls of a rule which is about which box it
 * is, {@linkplain GroundLevelBoxIdPointControls} (in the test sources of this module, next to this test).
 * <ul>
 * <li>Full support is an option of the packager: {@code withRequireFullSupport(true)}.
 * <li>Keeping boxes out of a part of the container is done with the obstacles of the container item. An obstacle which runs from the
 * floor to the roof is a column which no box can be placed in or over.
 * <li>A box which carries boxes of its own type only is a load constraint of the box: {@code withMaxLoadIdenticalBoxCount(-1)} (no
 * limit on the number of boxes on top, but they must be identical).
 * <li>A box which must be on the floor is a rule for which points are available to the box: point controls of the container item,
 * here for the box with a given id.
 * </ul>
 * Custom placement controls replace the default ones, and so do not retain such rules (see "Writing your own placement controls" in
 * <a href="../../../../../../../../../../DEVELOPER.md#writing-your-own-placement-controls">DEVELOPER.md</a>): configure the packager instead, as here.
 * <p>
 * Every test packs boxes into a single container. Each rule has a test which packs an input where the rule matters, next to the
 * same input without the rule. Another test combines the rules.
 * <p>
 * The layouts are checked with the validators module, which has the rules too: {@linkplain DefaultValidator} checks that the obstacles of
 * the result containers are left empty, and, for container items which have a placement validator builder factory (without one, loads and
 * stability are not validated), the load constraints which the boxes carry, such as identical boxes only. Full support is a stability rule
 * of the placement validator. The validator checks the rules it sees in the result or is told about, so a test which only calls it would
 * pass if the rule had been left out of the input. Each test therefore also checks one position directly, which differs without the rule,
 * and that the result has the obstacle.
 * <p>
 * The positions follow from how the plain packager ranks the choices, by default: the box with the largest volume (then the largest weight)
 * is placed first, and boxes which are alike in that are placed in the order they are listed in. A box which is not rotated is placed in
 * the lowest position where it fits, and a position which a rule excludes is skipped for the next lowest.
 */
class PlacementRulesExampleTest {

	private final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws IOException {
		validator.close();
	}

	/**
	 * Container 4 x 1 x 4. The cabinet is placed first (it has the largest volume), then the crate (it has the same volume as the board,
	 * and is heavier), then the board. The board is wider than the crate, and fits on top of the crate and on top of the cabinet. The
	 * lowest position is on the crate, where the board hangs over the floor next to it. With full support required, the board is placed
	 * on top of the cabinet.
	 */
	@Test
	void requireFullSupportKeepsABoxFromOverhanging() {
		Container container = container(4, 1, 4);
		BoxItem cabinet = new BoxItem(box("cabinet", 2, 1, 3, 3), 1);
		BoxItem crate = new BoxItem(box("crate", 1, 1, 2, 2), 1);
		BoxItem board = new BoxItem(box("board", 2, 1, 1, 1), 1);

		PackagerResult overhanging = pack(false, null, container, null, cabinet, crate, board);
		// without the requirement, the board rests on the crate, and hangs over the floor next to it
		// <figure>
		//   z                                                 z
		//                                                     3 +-------------------+-------------------+
		//   | /-------------------/-------------------|         |                   |                   |
		//   |/                   /                   /|         |                   |       board       |
		// 3 |-------------------|-------------------| |         |                   |                   |
		//   |                   |                   | |       2 |                   +---------+---------+
		//   |                   |       board       | |         |                   |         |
		//   |                   |                   |/          |      cabinet      |         |
		// 2 |                   |---------|---------|           |                   |         |
		//   |                   |         | |                   |                   |  crate  |
		//   |      cabinet      |         | |             y     |                   |         |
		//   |                   |         | |                   |                   |         |
		//   |                   |  crate  | |           /       |                   |         |
		//   |                   |         | |          /      0 +-------------------+---------+
		//   |                   |         | |         / 1       0                   2         3         4   x
		//   |                   |         |/         /
		// 0 |-------------------|---------|------------ x
		//   0                   2         3         4
		//
		// y                                                 z
		// 1 +-------------------+-------------------+       3 +---------+
		//   |                   |                   |         |         |
		//   |      cabinet      |       board       |         |  board  |
		//   |                   |                   |         |         |
		// 0 +-------------------+-------------------+       2 +---------+
		//   0                   2         3         4   x     |         |
		//                                                     |         |
		//                                                     |         |
		//                                                     |  crate  |
		//                                                     |         |
		//                                                     |         |
		//                                                     |         |
		//                                                   0 +---------+
		//                                                     0         1   y
		// </figure>
		figure(overhanging);

		PackagerResultAssert.assertThat(overhanging).isSuccess().hasContainerCount(1);
		assertThat(placement(overhanging, "board").getAbsoluteZ()).isEqualTo(2);
		// the validator knows what full support is: it rejects this layout
		PackagerResultAssert.assertThat(overhanging).isRejectedBy(validation(container, true, cabinet, crate, board), InsufficientSupportAreaReason.class);

		PackagerResult supported = pack(true, null, container, null, cabinet, crate, board);
		// with the requirement, the board is on the cabinet
		// <figure>
		//   z                                       z                                       y                                       z
		//                                           4 +-------------------+                 1 +-------------------+---------+       4 +---------+
		//   | /-------------------|                   |                   |                   |                   |         |         |         |
		//   |/                   /|                   |       board       |                   |       board       |  crate  |         |  board  |
		// 4 |-------------------| |                   |                   |                   |                   |         |         |         |
		//   |                   | |                 3 +-------------------+                 0 +-------------------+---------+       3 +---------+
		//   |       board       | |                   |                   |                   0                   2         3   x     |         |
		//   |                   |/|                   |                   |                                                           | cabinet |
		// 3 |-------------------| |                   |                   |                                                           |         |
		//   |                   | |                 2 |                   +---------+                                               2 +---------+
		//   |                   | |---------|         |                   |         |                                                 |         |
		//   |                   |/         /|         |      cabinet      |         |                                                 |         |
		// 2 |                   |---------| |         |                   |         |                                                 |         |
		//   |                   |         | |         |                   |  crate  |                                                 |  crate  |
		//   |      cabinet      |         | |   y     |                   |         |                                                 |         |
		//   |                   |         | |         |                   |         |                                                 |         |
		//   |                   |  crate  | | /       |                   |         |                                                 |         |
		//   |                   |         | |/      0 +-------------------+---------+                                               0 +---------+
		//   |                   |         | | 1       0                   2         3   x                                             0         1   y
		//   |                   |         |/
		// 0 |-------------------|---------|-- x
		//   0                   2         3
		// </figure>
		figure(supported);

		PackagerResultAssert.assertThat(supported).isSuccess().hasContainerCount(1);
		assertThat(placement(supported, "board").getAbsoluteZ()).isEqualTo(3);
		PackagerResultAssert.assertThat(supported).isAcceptedBy(validation(container, true, cabinet, crate, board));
	}

	/**
	 * Container 2 x 1 x 3, with two crates which take up the floor. The drum is stacked on one of the crates, unless the point controls of
	 * the container item only give it the points on the floor: then there is no place for it. With only one crate, there is floor left.
	 */
	@Test
	void groundOnlyBoxIsPlacedOnTheFloor() {
		Container container = container(2, 1, 3);
		BoxItem crates = new BoxItem(box("crate", 1, 1, 2, 2), 2);
		BoxItem drum = new BoxItem(cube("drum"), 1);

		PackagerResult stacked = pack(false, null, container, null, crates, drum);
		// without the point controls, the drum is stacked on a crate
		// <figure>
		//   z                         z                         y                         z
		//                             3 +-------+               1 +-------+-------+       3 +-------+
		//   | /-------|                 |       |                 |       |       |         |       |
		//   |/       /|                 | drum  |                 | drum  | crate |         | drum  |
		// 3 |-------| |                 |       |                 |       |       |         |       |
		//   |       | |               2 +-------+-------+       0 +-------+-------+       2 +-------+
		//   | drum  | |-------|         |       |       |         0       1       2   x     |       |
		//   |       |/       /|         |       |       |                                   |       |
		// 2 |-------|-------| |         |       |       |                                   |       |
		//   |       |       | |         | crate | crate |                                   | crate |
		//   |       |       | |   y     |       |       |                                   |       |
		//   |       |       | |         |       |       |                                   |       |
		//   | crate | crate | | /       |       |       |                                   |       |
		//   |       |       | |/      0 +-------+-------+                                 0 +-------+
		//   |       |       | | 1       0       1       2   x                               0       1   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(stacked);

		PackagerResultAssert.assertThat(stacked).isSuccess().hasContainerCount(1);
		assertThat(placement(stacked, "drum").getAbsoluteZ()).isEqualTo(2);

		// the drum must be on the floor, which the crates take up: there is no layout
		PackagerResult noFloor = pack(false, groundLevelDrums(), container, null, crates, drum);
		PackagerResultAssert.assertThat(noFloor).isNotSuccess();

		BoxItem crate = new BoxItem(box("crate", 1, 1, 2, 2), 1);
		PackagerResult onTheFloor = pack(false, groundLevelDrums(), container, null, crate, drum);
		// with room on the floor, the drum is placed there
		// <figure>
		//   z                         z                         y                         z
		//                             2 +-------+               1 +-------+-------+       2 +-------+
		//   | /-------|                 |       |                 |       |       |         |       |
		//   |/       /|                 |       |                 | crate | drum  |         | crate |
		// 2 |-------| |                 |       |                 |       |       |         |       |
		//   |       | |               1 | crate +-------+       0 +-------+-------+       1 +-------+
		//   |       | |-------|   y     |       |       |         0       1       2   x     |       |
		//   |       |/       /|         |       | drum  |                                   | drum  |
		// 1 | crate |-------| | /       |       |       |                                   |       |
		//   |       |       | |/      0 +-------+-------+                                 0 +-------+
		//   |       | drum  | | 1       0       1       2   x                               0       1   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(onTheFloor);

		PackagerResultAssert.assertThat(onTheFloor).isSuccess().hasContainerCount(1);
		assertThat(placement(onTheFloor, "drum").getAbsoluteZ()).isEqualTo(0);
	}

	/**
	 * Container 3 x 1 x 3. The crate is placed first (it is heavier than the tray), then the tray beside it, then the cup. The lowest
	 * position of the cup is on the tray, unless the tray carries identical boxes only: then the cup is placed on the crate.
	 */
	@Test
	void identicalOnlyBoxCarriesOnlyTheSameType() {
		Container container = container(3, 1, 3);
		BoxItem crate = new BoxItem(box("crate", 1, 1, 2, 2), 1);
		BoxItem cup = new BoxItem(cube("cup"), 1);

		BoxItem plainTray = new BoxItem(box("tray", 2, 1, 1, 1), 1);
		PackagerResult mixed = pack(false, null, container, null, crate, plainTray, cup);
		// without the constraint, the cup is stacked on the tray
		// <figure>
		//   z                                 z                                 y                                 z
		//                                     2 +-------+-------+               1 +-------+-------+-------+       2 +-------+
		//   | /-------/-------|                 |       |       |                 |       |       |       |         |       |
		//   |/       /       /|                 |       |  cup  |                 | crate |  cup  | tray  |         |  cup  |
		// 2 |-------|-------| |                 |       |       |                 |       |       |       |         |       |
		//   |       |       | |               1 | crate +-------+-------+       0 +-------+-------+-------+       1 +-------+
		//   |       |  cup  | |-------|   y     |       |               |         0       1       2       3   x     |       |
		//   |       |       |/       /|         |       |     tray      |                                           | tray  |
		// 1 | crate |-------|-------| | /       |       |               |                                           |       |
		//   |       |               | |/      0 +-------+---------------+                                         0 +-------+
		//   |       |     tray      | | 1       0       1       2       3   x                                       0       1   y
		//   |       |               |/
		// 0 |-------|---------------|-- x
		//   0       1       2       3
		// </figure>
		figure(mixed);

		PackagerResultAssert.assertThat(mixed).isSuccess().hasContainerCount(1);
		assertThat(placement(mixed, "cup").getAbsoluteZ()).isEqualTo(1);

		BoxItem identicalTray = new BoxItem(Box.newBuilder().withId("tray").withSize(2, 1, 1).withWeight(1).withMaxLoadIdenticalBoxCount(-1).build(), 1);
		PackagerResult identicalOnly = pack(false, null, container, null, crate, identicalTray, cup);
		// the tray carries identical boxes only: the cup is stacked on the crate
		// <figure>
		//   z                                 z                                 y                                 z
		//                                     3 +-------+                       1 +-------+---------------+       3 +-------+
		//   | /-------|                         |       |                         |       |               |         |       |
		//   |/       /|                         |  cup  |                         |  cup  |     tray      |         |  cup  |
		// 3 |-------| |                         |       |                         |       |               |         |       |
		//   |       | |                       2 +-------+                       0 +-------+---------------+       2 +-------+
		//   |  cup  | |                         |       |                         0       1               3   x     |       |
		//   |       |/|                         |       |                                                           | crate |
		// 2 |-------| |                         |       |                                                           |       |
		//   |       | |                       1 | crate +---------------+                                         1 +-------+
		//   |       | |---------------|   y     |       |               |                                           |       |
		//   |       |/               /|         |       |     tray      |                                           | tray  |
		// 1 | crate |---------------| | /       |       |               |                                           |       |
		//   |       |               | |/      0 +-------+---------------+                                         0 +-------+
		//   |       |     tray      | | 1       0       1               3   x                                       0       1   y
		//   |       |               |/
		// 0 |-------|---------------|-- x
		//   0       1               3
		// </figure>
		figure(identicalOnly);

		PackagerResultAssert.assertThat(identicalOnly).isSuccess().hasContainerCount(1);
		assertThat(placement(identicalOnly, "cup").getAbsoluteZ()).isEqualTo(2);
		PackagerResultAssert.assertThat(identicalOnly).isAcceptedBy(validation(container, false, crate, identicalTray, cup));
	}

	/**
	 * Container 2 x 2 x 2 for six cubes. The obstacle (#), which is given with the container item, is a column at x = 1, y = 1, from the
	 * floor to the roof. Without it, the cubes take up the floor and then the level above; with it, the column is left empty.
	 */
	@Test
	void obstaclesOfTheContainerAreLeftEmpty() {
		Container container = container(2, 2, 2);
		BoxItem cubes = new BoxItem(cube("cube"), 6);

		PackagerResult withoutObstacle = pack(false, null, container, null, cubes);
		// without the obstacle, a cube takes up the column
		// <figure>
		//   z   /-------|               z                         y                         z
		//      / cube  /|               2 +-------+               2 +-------+-------+       2 +-------+-------+
		//   | /-------| |                 |       |                 |       |       |         |       |       |
		//   |/       /| |                 | cube  |                 | cube  | cube  |         | cube  | cube  |
		// 2 |-------| | |-------|   y     |       |                 |       |       |         |       |       |
		//   |       | |/ cube  /|       1 +-------+-------+       1 +-------+-------+       1 +-------+-------+
		//   | cube  | |-------| | /       |       |       |         |       |       |         |       |       |
		//   |       |/       /| |/        | cube  | cube  |         | cube  | cube  |         | cube  | cube  |
		// 1 |-------|-------| | | 2       |       |       |         |       |       |         |       |       |
		//   |       |       | |/        0 +-------+-------+       0 +-------+-------+       0 +-------+-------+
		//   | cube  | cube  | | 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(withoutObstacle);

		PackagerResultAssert.assertThat(withoutObstacle).isSuccess().hasContainerCount(1);
		assertThat(placements(withoutObstacle)).anyMatch(p -> p.getAbsoluteX() == 1 && p.getAbsoluteY() == 1);

		PackagerResult result = pack(false, null, container, column(container, 1, 1), cubes);
		// the obstacle is drawn as #
		// <figure>
		//   z   /-------/-------|       z                         y                         z
		//      / cube  /#######/|       2 +-------+-------+       2 +-------+-------+       2 +-------+-------+
		//   | /-------/-------|#|         |       |       |         |       |#######|         |       |#######|
		//   |/       /       /|#|         | cube  | cube  |         | cube  |#######|         | cube  |#######|
		// 2 |-------|-------| |#|   y     |       |       |         |       |#######|         |       |#######|
		//   |       |       | |#|       1 +-------+-------+       1 +-------+-------+       1 +-------+#######|
		//   | cube  | cube  | |#| /       |       |       |         |       |       |         |       |#######|
		//   |       |       |/|#|/        | cube  | cube  |         | cube  | cube  |         | cube  |#######|
		// 1 |-------|-------| |#| 2       |       |       |         |       |       |         |       |#######|
		//   |       |       | |/        0 +-------+-------+       0 +-------+-------+       0 +-------+-------+
		//   | cube  | cube  | | 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(result);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(1).hasStackSize(0, 6);
		// the result has the obstacle, from the floor to the roof: it is what the validator checks the boxes against
		assertThat(result.get(0).getObstacles()).singleElement().satisfies(obstacle -> {
			assertThat(obstacle.getAbsoluteZ()).isZero();
			assertThat(obstacle.getAbsoluteEndZ()).isEqualTo(container.getLoadDz() - 1);
		});
		PackagerResultAssert.assertThat(result).isAcceptedBy(validation(container, false, cubes));
	}

	/**
	 * Container 5 x 1 x 5 with an obstacle at x = 4, and all the rules at once. What is left is 4 x 1 x 5. The cabinet is placed first, then
	 * the crate beside it, then the board, which is wider than the floor left next to the crate, and hangs over it if placed on the crate:
	 * it is placed on the cabinet. The drum, which may only be on the floor, takes the floor next to the crate, and the tray is stacked on
	 * it. The cup is not stacked on the tray, which carries identical boxes only, but on the crate. Full support is required, and holds.
	 */
	@Test
	void theRulesCanBeCombined() {
		Container container = container(5, 1, 5);
		List<BoxItem> boxItems = List.of(
				new BoxItem(box("cabinet", 2, 1, 4, 4), 1),
				new BoxItem(box("crate", 1, 1, 3, 3), 1),
				new BoxItem(box("board", 2, 1, 1, 1), 1),
				new BoxItem(cube("drum", 3), 1),
				new BoxItem(Box.newBuilder().withId("tray").withSize(1, 1, 1).withWeight(2).withMaxLoadIdenticalBoxCount(-1).build(), 1),
				new BoxItem(cube("cup"), 1));

		PackagerResult result = pack(true, groundLevelDrums(), container, column(container, 4, 0), boxItems.toArray(new BoxItem[0]));
		// <figure>
		//   z                                                 z
		//                                                     5 +---------------+               +-------+
		//   | /---------------|               /-------|         |               |               |#######|
		//   |/               /|              /#######/|         |     board     |               |#######|
		// 5 |---------------| |             |-------|#|         |               |               |#######|
		//   |               | |             |#######|#|       4 +---------------+-------+       |#######|
		//   |     board     | |-------|     |#######|#|         |               |       |       |#######|
		//   |               |/       /|     |#######|#|         |               |  cup  |       |#######|
		// 4 |---------------|-------| |     |#######|#|         |               |       |       |#######|
		//   |               |       | |     |#######|#|       3 |               +-------+       |#######|
		//   |               |  cup  | |     |#######|#|         |               |       |       |#######|
		//   |               |       |/|     |#######|#|         |               |       |       |#######|
		// 3 |               |-------| |     |#######|#|         |               |       |       |#######|
		//   |               |       | |     |#######|#|       2 |    cabinet    |       +-------+#######|
		//   |               |       | |-----|#######|#|         |               |       |       |#######|
		//   |               |       |/      |#######|#|         |               | crate | tray  |#######|
		// 2 |    cabinet    |       |-------|#######|#|         |               |       |       |#######|
		//   |               |       |       |#######|#|       1 |               |       +-------+#######|
		//   |               | crate | tray  |#######|#|   y     |               |       |       |#######|
		//   |               |       |       |#######|#|         |               |       | drum  |#######|
		// 1 |               |       |-------|#######|#| /       |               |       |       |#######|
		//   |               |       |       |#######|#|/      0 +---------------+-------+-------+-------+
		//   |               |       | drum  |#######|#| 1       0               2       3       4       5   x
		//   |               |       |       |#######|/
		// 0 |---------------|-------|-------|-------|-- x
		//   0               2       3       4       5
		//
		// y                                                 z
		// 1 +---------------+-------+-------+-------+       5 +-------+
		//   |               |       |       |#######|         |#######|
		//   |     board     |  cup  | tray  |#######|         |#######|
		//   |               |       |       |#######|         |#######|
		// 0 +---------------+-------+-------+-------+       4 |#######|
		//   0               2       3       4       5   x     |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                   3 |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                   2 |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                   1 |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                     |#######|
		//                                                   0 +-------+
		//                                                     0       1   y
		// </figure>
		figure(result);

		PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(1).hasStackSize(0, 6);
		// these positions differ without the rules (the validator also sees them: the obstacle in the result container, the identical-only
		// tray in the stack values and the support from the stability validator)
		assertThat(placement(result, "board").getAbsoluteZ()).isEqualTo(4);
		assertThat(placement(result, "cup").getAbsoluteZ()).isEqualTo(3);
		assertThat(result.get(0).getObstacles()).hasSize(1);
		// the drum is on the floor (the validator does not know this rule, and it would be there without it too: a rule which keeps a box on
		// the floor can only make a layout impossible, see groundOnlyBoxIsPlacedOnTheFloor)
		assertThat(placement(result, "drum").getAbsoluteZ()).isZero();
		PackagerResultAssert.assertThat(result).isAcceptedBy(validation(container, true, boxItems.toArray(new BoxItem[0])));
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// input
	// ------------------------------------------------------------------------------------------------------------------------------

	private static PointControlsBuilderFactory groundLevelDrums() {
		return GroundLevelBoxIdPointControls.newFactory("drum");
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
	 * @return an obstacle of one box by one box, from the floor to the roof of the container: x, y, z, dx, dy and dz
	 */
	private static int[] column(Container container, int x, int y) {
		return new int[] { x, y, 0, 1, 1, container.getLoadDz() };
	}

	/**
	 * @param requireFullSupport the option of the packager
	 * @param pointControls point controls of the container item, or null for none
	 * @param obstacle x, y, z, dx, dy and dz of an obstacle in the container, or null for none
	 */
	private static PackagerResult pack(boolean requireFullSupport, PointControlsBuilderFactory pointControls, Container container, int[] obstacle, BoxItem... boxItems) {
		try (PlainPackager packager = PlainPackager.newBuilder().withRequireFullSupport(requireFullSupport).build()) {
			return packager.newResultBuilder()
					.withContainerItem(b -> {
						b.withContainerItem(container, 1);
						if(pointControls != null) {
							b.withPointControlsBuilderFactory(pointControls);
						}
						if(obstacle != null) {
							b.withObstacles(o -> o.withObstacle(obstacle[0], obstacle[1], obstacle[2], obstacle[3], obstacle[4], obstacle[5]));
						}
					})
					.withMaxContainerCount(1)
					.withBoxItems(boxItems)
					.withInterruptDuration(10_000)
					.build();
		}
	}

	/**
	 * The validator, for the same single container and boxes as the packager: it finds the obstacles in the result containers, and the load
	 * constraints in the box stack values of the placements.
	 *
	 * @param requireFullSupport whether to also validate that every box is fully supported
	 */
	private ValidatorResultBuilder validation(Container container, boolean requireFullSupport, BoxItem... boxItems) {
		return validator.newResultBuilder()
				.withContainerItem(b -> b.withContainerItem(container, 1).withPlacementValidatorBuilderFactory(f -> {
					if(requireFullSupport) {
						f.withStabilityValidator(new FullySupportedStabilityValidator());
					}
				}))
				.withMaxContainerCount(1)
				.withBoxItems(boxItems);
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
