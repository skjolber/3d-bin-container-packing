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
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;
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
 * <li>A box which carries boxes of its own type only is a load constraint of the box: {@code withMaxLoadIdenticalBoxCount(count)}. The
 * boxes on top must be identical, and there are at most {@code count} levels of them (through all the boxes above; -1 for no limit).
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
 * the result containers are left empty, and the load constraints which the boxes carry, such as identical boxes only, with no configuration
 * (the load constraints are validated by default). Full support is a stability rule, which is a policy, so it is added to the container item with a
 * placement validator builder factory which has a stability validator. The validator checks the rules it sees in the result or is told
 * about, so a test which only calls it would pass if the rule had been left out of the input. Each test therefore also checks one position
 * directly, which differs without the rule, and that the result has the obstacle.
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
	 * Container 3 x 1 x 6 for a crate, four trays and a cup. The crate is placed first (it has the largest volume), and takes up two
	 * thirds of the floor; the trays are placed next (they are heavier than the cup), the cup last. Each box is placed in the lowest
	 * position where it fits: the trays are stacked in the column beside the crate, and the cup on top of them, as that is lower than
	 * the top of the crate.
	 * <p>
	 * If the trays carry identical boxes only ({@code withMaxLoadIdenticalBoxCount(-1)}), they are still stacked on each other, but the
	 * cup is placed on the crate. If they also carry two levels of boxes at most ({@code withMaxLoadIdenticalBoxCount(2)}), the first tray
	 * carries the second and the third, but not the fourth: the levels are counted through all the boxes above. The fourth tray is placed
	 * on the crate instead, next to the cup.
	 * <p>
	 * The validator rejects layouts which break the rules: a tower of four trays, and a cup on a tray. The packager does not build
	 * them, so they are laid out by hand.
	 */
	@Test
	void identicalBoxCarriesOnlyTheSameTypeUpToTheMaximumCount() {
		Container container = container(3, 1, 6);
		BoxItem crate = new BoxItem(box("crate", 2, 1, 5, 5), 1);
		BoxItem cup = new BoxItem(cube("cup"), 1);

		BoxItem plainTrays = new BoxItem(cube("tray", 2), 4);
		PackagerResult mixed = pack(false, null, container, null, crate, plainTrays, cup);
		// without the constraint, the trays are stacked four high, and the cup is stacked on top of them
		// <figure>
		//   z                                 z                                 y                                 z
		//                                     5 +---------------+-------+       1 +---------------+-------+       5 +-------+
		//   | /---------------/-------|         |               |       |         |               |       |         |       |
		//   |/               /       /|         |               |  cup  |         |     crate     |  cup  |         |  cup  |
		// 5 |---------------|-------| |         |               |       |         |               |       |         |       |
		//   |               |       | |       4 |               +-------+       0 +---------------+-------+       4 +-------+
		//   |               |  cup  | |         |               |       |         0               2       3   x     |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 4 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       3 |               +-------+                                         3 +-------+
		//   |               | tray  | |         |               |       |                                           |       |
		//   |               |       |/|         |     crate     | tray  |                                           | tray  |
		// 3 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       2 |               +-------+                                         2 +-------+
		//   |     crate     | tray  | |         |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 2 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       1 |               +-------+                                         1 +-------+
		//   |               | tray  | |   y     |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 1 |               |-------| | /       |               |       |                                           |       |
		//   |               |       | |/      0 +---------------+-------+                                         0 +-------+
		//   |               | tray  | | 1       0               2       3   x                                       0       1   y
		//   |               |       |/
		// 0 |---------------|-------|-- x
		//   0               2       3
		// </figure>
		figure(mixed);

		PackagerResultAssert.assertThat(mixed).isSuccess().hasContainerCount(1);
		assertThat(placements(mixed, "tray")).extracting(Placement::getAbsoluteZ).containsExactlyInAnyOrder(0, 1, 2, 3);
		assertThat(placement(mixed, "cup").getAbsoluteZ()).isEqualTo(4);

		BoxItem identicalTrays = new BoxItem(tray(-1), 4);
		PackagerResult identicalOnly = pack(false, null, container, null, crate, identicalTrays, cup);
		// the trays carry identical boxes only: they are still stacked four high, but the cup is on the crate
		// <figure>
		//   z                                 z                                 y                                 z
		//                                     6 +-------+                       1 +-------+-------+-------+       6 +-------+
		//   | /-------|                         |       |                         |       |       |       |         |       |
		//   |/       /|                         |  cup  |                         |  cup  | crate | tray  |         |  cup  |
		// 6 |-------| |                         |       |                         |       |       |       |         |       |
		//   |       | |                       5 +-------+-------+               0 +-------+-------+-------+       5 +-------+
		//   |  cup  | |-------|                 |               |                 0       1       2       3   x     |       |
		//   |       |/       /|                 |               |                                                   | crate |
		// 5 |-------|-------| |                 |               |                                                   |       |
		//   |               | |               4 |               +-------+                                         4 +-------+
		//   |               | |-------|         |               |       |                                           |       |
		//   |               |/       /|         |               | tray  |                                           | tray  |
		// 4 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       3 |               +-------+                                         3 +-------+
		//   |               | tray  | |         |               |       |                                           |       |
		//   |               |       |/|         |     crate     | tray  |                                           | tray  |
		// 3 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       2 |               +-------+                                         2 +-------+
		//   |     crate     | tray  | |         |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 2 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       1 |               +-------+                                         1 +-------+
		//   |               | tray  | |   y     |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 1 |               |-------| | /       |               |       |                                           |       |
		//   |               |       | |/      0 +---------------+-------+                                         0 +-------+
		//   |               | tray  | | 1       0       1       2       3   x                                       0       1   y
		//   |               |       |/
		// 0 |---------------|-------|-- x
		//   0       1       2       3
		// </figure>
		figure(identicalOnly);

		PackagerResultAssert.assertThat(identicalOnly).isSuccess().hasContainerCount(1);
		assertThat(placements(identicalOnly, "tray")).extracting(Placement::getAbsoluteZ).containsExactlyInAnyOrder(0, 1, 2, 3);
		assertThat(placement(identicalOnly, "cup").getAbsoluteZ()).isEqualTo(5);
		// the trays do carry trays, and trays only (in the support graph which the validator uses)
		assertThat(boxesOnTrays(identicalOnly)).isNotEmpty().extracting(placement -> placement.getBox().getId()).containsOnly("tray");
		assertThat(levelsOnTrays(identicalOnly)).isEqualTo(3);
		PackagerResultAssert.assertThat(identicalOnly).isAcceptedBy(validation(container, false, crate, identicalTrays, cup));

		BoxItem trays = new BoxItem(tray(2), 4);
		PackagerResult capped = pack(false, null, container, null, crate, trays, cup);
		// the trays also carry two levels at most: three are stacked, the fourth is on the crate, next to the cup
		// <figure>
		//   z                                 z                                 y                                 z
		//                                     6 +-------+-------+               1 +-------+-------+-------+       6 +-------+
		//   | /-------/-------|                 |       |       |                 |       |       |       |         |       |
		//   |/       /       /|                 | tray  |  cup  |                 | tray  |  cup  | tray  |         |  cup  |
		// 6 |-------|-------| |                 |       |       |                 |       |       |       |         |       |
		//   |       |       | |               5 +-------+-------+               0 +-------+-------+-------+       5 +-------+
		//   | tray  |  cup  | |                 |               |                 0       1       2       3   x     |       |
		//   |       |       |/|                 |               |                                                   |       |
		// 5 |-------|-------| |                 |               |                                                   |       |
		//   |               | |                 |               |                                                   | crate |
		//   |               | |                 |               |                                                   |       |
		//   |               | |                 |               |                                                   |       |
		//   |               | |                 |               |                                                   |       |
		//   |               | |               3 |               +-------+                                         3 +-------+
		//   |               | |-------|         |               |       |                                           |       |
		//   |               |/       /|         |     crate     | tray  |                                           | tray  |
		// 3 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       2 |               +-------+                                         2 +-------+
		//   |     crate     | tray  | |         |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 2 |               |-------| |         |               |       |                                           |       |
		//   |               |       | |       1 |               +-------+                                         1 +-------+
		//   |               | tray  | |   y     |               |       |                                           |       |
		//   |               |       |/|         |               | tray  |                                           | tray  |
		// 1 |               |-------| | /       |               |       |                                           |       |
		//   |               |       | |/      0 +---------------+-------+                                         0 +-------+
		//   |               | tray  | | 1       0       1       2       3   x                                       0       1   y
		//   |               |       |/
		// 0 |---------------|-------|-- x
		//   0       1       2       3
		// </figure>
		figure(capped);

		PackagerResultAssert.assertThat(capped).isSuccess().hasContainerCount(1);
		assertThat(placements(capped, "tray")).extracting(Placement::getAbsoluteZ).containsExactlyInAnyOrder(0, 1, 2, 5);
		assertThat(placement(capped, "cup").getAbsoluteZ()).isEqualTo(5);
		// still trays on trays, only trays, and the most levels on top of a tray are the maximum
		assertThat(boxesOnTrays(capped)).isNotEmpty().extracting(placement -> placement.getBox().getId()).containsOnly("tray");
		assertThat(levelsOnTrays(capped)).isEqualTo(2);
		PackagerResultAssert.assertThat(capped).isAcceptedBy(validation(container, false, crate, trays, cup));

		// the fourth tray on the tower: the first tray carries three levels
		PackagerResult tower = layout(container,
				place(crate, 0, 0, 0), place(trays, 2, 0, 0), place(trays, 2, 0, 1), place(trays, 2, 0, 2), place(trays, 2, 0, 3), place(cup, 0, 0, 5));
		PackagerResultAssert.assertThat(tower).isRejectedBy(validation(container, false, crate, trays, cup), ExcessiveLoadBoxCountReason.class);

		// the cup on the second tray, in place of the third tray: it rests on trays which carry trays only
		PackagerResult cupOnTray = layout(container,
				place(crate, 0, 0, 0), place(trays, 2, 0, 0), place(trays, 2, 0, 1), place(cup, 2, 0, 2), place(trays, 0, 0, 5), place(trays, 1, 0, 5));
		PackagerResultAssert.assertThat(cupOnTray).isRejectedBy(validation(container, false, crate, trays, cup), NonIdenticalLoadBoxReason.class, NonIdenticalLoadBoxReason.class);
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
	 * @param maxLoadIdenticalBoxCount the most levels of identical boxes on top of the tray, or -1 for no limit
	 * @return a tray, which carries identical boxes only
	 */
	private static Box tray(int maxLoadIdenticalBoxCount) {
		return Box.newBuilder()
				.withId("tray")
				.withSize(1, 1, 1)
				.withWeight(2)
				.withMaxLoadIdenticalBoxCount(maxLoadIdenticalBoxCount)
				.build();
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
	 * constraints in the box stack values of the placements (validated by default).
	 *
	 * @param requireFullSupport whether to also validate that every box is fully supported: a stability rule, so the container item is
	 *        given a placement validator builder factory with the stability validator
	 */
	private ValidatorResultBuilder validation(Container container, boolean requireFullSupport, BoxItem... boxItems) {
		return validator.newResultBuilder()
				.withContainerItem(b -> {
					b.withContainerItem(container, 1);
					if(requireFullSupport) {
						b.withPlacementValidatorBuilderFactory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator()));
					}
				})
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

	/** The placements of the boxes with an id */
	private static List<Placement> placements(PackagerResult result, String boxId) {
		List<Placement> placements = new ArrayList<>();
		for(Placement placement : placements(result)) {
			if(boxId.equals(placement.getBox().getId())) {
				placements.add(placement);
			}
		}
		return placements;
	}

	/** The boxes which rest directly on a tray, in the support graph of the placements (the validators use it too) */
	private static List<Placement> boxesOnTrays(PackagerResult result) {
		SupportGraph graph = new SupportGraph(placements(result));
		List<Placement> placements = new ArrayList<>();
		for(Placement tray : placements(result, "tray")) {
			for(PlacementLoad load : graph.getSupportees(tray)) {
				placements.add(load.getPlacement());
			}
		}
		return placements;
	}

	/** The most levels of boxes on top of a tray, through all the boxes above it */
	private static int levelsOnTrays(PackagerResult result) {
		SupportGraph graph = new SupportGraph(placements(result));
		int levels = 0;
		for(Placement tray : placements(result, "tray")) {
			levels = Math.max(levels, levelsAbove(graph, tray));
		}
		return levels;
	}

	/** The levels of boxes on top of a placement: the longest chain of boxes which rest on each other */
	private static int levelsAbove(SupportGraph graph, Placement placement) {
		int levels = 0;
		for(PlacementLoad load : graph.getSupportees(placement)) {
			levels = Math.max(levels, 1 + levelsAbove(graph, load.getPlacement()));
		}
		return levels;
	}

	/** A box in the container, in its first position (rotation) */
	private static Placement place(BoxItem boxItem, int x, int y, int z) {
		return new Placement(boxItem, boxItem.getBox().getStackValue(0), 0, x, y, z);
	}

	/** A result with a single container with the placements (in the order of the stack: from the bottom up): a layout which the packager does not build */
	private static PackagerResult layout(Container container, Placement... placements) {
		Container result = container.copy();
		result.getStack().addAll(List.of(placements));
		return new PackagerResult(List.of(result), 0, false);
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
