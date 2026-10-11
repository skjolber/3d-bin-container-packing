package com.github.skjolber.packing.packer.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsOnGroundLevelPointControls;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * The squares of the Bouwkamp code 15x11A fill a 15 x 11 container exactly. The plain packager does not find
 * the arrangement, but brute force does. A larger container fits the squares with either packager:
 *
 * <pre>
 *   exact (15 x 11):  6 6 5 5 4 4 3 1 1, no gaps       larger (16 x 12): with gaps
 * </pre>
 */
public class CompositePackagerTest {

	private static final long INTERRUPT_DURATION = 10_000;

	private final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws Exception {
		validator.close();
	}

	@Test
	public void plainPackagerNeedsTheLargerContainer() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+-----------------------+-----------------+
			//                           /------------------/--|                          /       |         square-6          |       square-5        |    square-4     |
			//                          /                  /sq/|                         /      0 +---------------------------+-----------------------+-----------------+
			//                         /                  /--| |          /--------|    / 12      0                           6    7                  11            14  15   x
			//                        /                  /sq/|/          /        /|   /
			//                       /                  /--/------------/        / |  / 11
			//                      /                  /               / square / /  /
			//                     /     square-6     /               /        / /  / 10
			//                    /                  /               /        / /  /
			//                   /                  /               /--------/--| /
			//                  /                  /   square-5    /           /|/
			//                 /                  /               /           / | 8
			//                /                  /               /           / /
			//               /------------------/               / square-4  / /
			//              /                  /               /           / /
			//             /                  /---------------/           / / 6
			//            /                  /               /           / /
			//           /                  /               /-----------| / 5
			//          /                  /               /           /|/
			//         /                  /               /           / | 4
			//        /                  /               /           / /
			//   z   /                  /               /           / /
			//      /                  /               /           / /
			//   | /                  /               /           / /
			//   |/                  /               /           / /
			// 1 |------------------|---------------|-----------| /
			//   |     square-6     |   square-5    | square-4  |/
			// 0 |------------------|---------------|-----------|-- x
			//   0                  6              11          15
			//
			// y                                                                                z
			// 12 +---------------------------+---+                                             1 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                                               |       square-4       |       square-4        |    square-3     | sq |
			//    |                           |   |                                               |                      |                       |                 |    |
			// 11 |                           +---+                  +-------------+            0 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                  |             |              0                      4     5     6           8           10    11   12   y
			// 10 |                           +---+------------------+             |
			//    |                           |                      |  square-3   |
			//    |         square-6          |                      |             |
			//    |                           |                      |             |
			//  8 |                           |                      +-------------+---+
			//    |                           |       square-5       |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  6 +---------------------------+                      |    square-4     |
			//    |                           |                      |                 |
			//  5 |                           +----------------------+                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  4 |                           |                      +-----------------+
			//    |                           |                      |                 |
			//    |         square-6          |                      |                 |
			//    |                           |       square-5       |                 |
			//    |                           |                      |    square-4     |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  0 +---------------------------+----------------------+-----------------+
			//    0                           6   7                  11            14  15   x
			// </figure>
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
	}

	@Test
	public void costlyPackagerFindsTheExactContainer() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterruptDuration(INTERRUPT_DURATION).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+------------------+----------------------+
			//                         /---------------/-------------/------------------| /       |         square-6          |     square-4     |       square-5       |
			//                        /               /             /                  /|/      0 +---------------------------+------------------+----------------------+
			//                       /               /             /                  / | 11      0                      5    6             9    10                     15   x
			//                      /               /             /                  / /
			//                     /               /  square-4   /                  / /
			//                    /   square-5    /             /                  / /
			//                   /               /             /     square-6     / /
			//                  /               /             /                  / /
			//                 /               /---/---------/                  / /
			//                /               / s /         /                  / /
			//               /---------------/---/         /                  / / 7
			//              /                   / square- /                  / /
			//             /                   /         /--/---------------| / 6
			//            /                   /         /sq/               /|/
			//           /                   /---------/--/               / | 5
			//          /                   /            /               / /
			//         /                   /            /               / / 4
			//        /                   /            /               / /
			//   z   /                   /            /               / /
			//      /                   /            /               / /
			//   | /                   /            /               / /
			//   |/                   /            /               / /
			// 1 |-------------------|------------|---------------| /
			//   |     square-6      |  square-4  |   square-5    |/
			// 0 |-------------------|------------|---------------|-- x
			//   0                   6           10              15
			//
			// y                                                                                z
			// 11 +----------------------+------------------+--------------------------+        1 +-------------------------------+-------------------------------------+
			//    |                      |                  |                          |          |           square-5            |              square-6               |
			//    |                      |                  |                          |          |                               |                                     |
			//    |                      |                  |                          |        0 +-------------------------------+-------------------------------------+
			//    |                      |     square-4     |                          |          0                         4     5     6      7                        11   y
			//    |       square-5       |                  |                          |
			//    |                      |                  |         square-6         |
			//    |                      |                  |                          |
			//    |                      |                  |                          |
			//  7 |                      +----+-------------+                          |
			//    |                      | sq |             |                          |
			//  6 +----------------------+----+             |                          |
			//    |                           |  square-3   |                          |
			//  5 |                           |             +---+----------------------+
			//    |                           |             | s |                      |
			//    |                           |             |   |                      |
			//  4 |                           +-------------+---+                      |
			//    |                           |                 |                      |
			//    |         square-6          |                 |                      |
			//    |                           |                 |       square-5       |
			//    |                           |    square-4     |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//  0 +---------------------------+-----------------+----------------------+
			//    0                      5    6             9   10                     15   x
			// </figure>
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("exact");
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(1)
					.withBoxItems(boxItems));
		}
	}

	@Test
	public void costlyPackagerIsNotUsedWhereTheCheapPackagerPacksAllBoxes() throws Exception {
		//  [a][b]  plain packs both boxes, so brute force is never attempted
		List<ContainerItem> containers = List.of(new ContainerItem(container("row", 2, 1), 1));
		List<BoxItem> boxItems = List.of(new BoxItem(square(1), 2));
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();
			// <figure>
			//   z                               z                               y                               z
			//                                   1 +----------+----------+       1 +----------+----------+       1 +----------+
			//   | /----------/----------|   y     |          |          |         |          |          |         |          |
			//   |/          /          /|         | square-1 | square-1 |         | square-1 | square-1 |         | square-1 |
			// 1 |----------|----------| | /       |          |          |         |          |          |         |          |
			//   |          |          | |/      0 +----------+----------+       0 +----------+----------+       0 +----------+
			//   | square-1 | square-1 | | 1       0          1          2   x     0          1          2   x     0          1   y
			//   |          |          |/
			// 0 |----------|----------|-- x
			//   0          1          2
			// </figure>
			figure(result);

			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(1)
					.withBoxItems(boxItems));
		}
		assertThat(costly.getAttempts()).isZero();
	}

	@Test
	public void returnsTheBaselineWhenTheImprovementIsInterrupted() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		AtomicBoolean interrupted = new AtomicBoolean();
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager(() -> {
			interrupted.set(true);
			throw new PackagerInterruptedException();
		});
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterrupt(interrupted::get).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+-----------------------+-----------------+
			//                           /------------------/--|                          /       |         square-6          |       square-5        |    square-4     |
			//                          /                  /sq/|                         /      0 +---------------------------+-----------------------+-----------------+
			//                         /                  /--| |          /--------|    / 12      0                           6    7                  11            14  15   x
			//                        /                  /sq/|/          /        /|   /
			//                       /                  /--/------------/        / |  / 11
			//                      /                  /               / square / /  /
			//                     /     square-6     /               /        / /  / 10
			//                    /                  /               /        / /  /
			//                   /                  /               /--------/--| /
			//                  /                  /   square-5    /           /|/
			//                 /                  /               /           / | 8
			//                /                  /               /           / /
			//               /------------------/               / square-4  / /
			//              /                  /               /           / /
			//             /                  /---------------/           / / 6
			//            /                  /               /           / /
			//           /                  /               /-----------| / 5
			//          /                  /               /           /|/
			//         /                  /               /           / | 4
			//        /                  /               /           / /
			//   z   /                  /               /           / /
			//      /                  /               /           / /
			//   | /                  /               /           / /
			//   |/                  /               /           / /
			// 1 |------------------|---------------|-----------| /
			//   |     square-6     |   square-5    | square-4  |/
			// 0 |------------------|---------------|-----------|-- x
			//   0                  6              11          15
			//
			// y                                                                                z
			// 12 +---------------------------+---+                                             1 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                                               |       square-4       |       square-4        |    square-3     | sq |
			//    |                           |   |                                               |                      |                       |                 |    |
			// 11 |                           +---+                  +-------------+            0 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                  |             |              0                      4     5     6           8           10    11   12   y
			// 10 |                           +---+------------------+             |
			//    |                           |                      |  square-3   |
			//    |         square-6          |                      |             |
			//    |                           |                      |             |
			//  8 |                           |                      +-------------+---+
			//    |                           |       square-5       |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  6 +---------------------------+                      |    square-4     |
			//    |                           |                      |                 |
			//  5 |                           +----------------------+                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  4 |                           |                      +-----------------+
			//    |                           |                      |                 |
			//    |         square-6          |                      |                 |
			//    |                           |       square-5       |                 |
			//    |                           |                      |    square-4     |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  0 +---------------------------+----------------------+-----------------+
			//    0                           6   7                  11            14  15   x
			// </figure>
			figure(result);

			// the plain packager's result
			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
			assertThat(result.isTimeout()).isFalse();
		}
		assertThat(costly.getAttempts()).isEqualTo(1);
	}

	@Test
	public void stopsUsingAPackagerWhenItIsInterruptedByItsBudget() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager(() -> {
			throw new PackagerInterruptedException();
		});
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly, 60_000)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+-----------------------+-----------------+
			//                           /------------------/--|                          /       |         square-6          |       square-5        |    square-4     |
			//                          /                  /sq/|                         /      0 +---------------------------+-----------------------+-----------------+
			//                         /                  /--| |          /--------|    / 12      0                           6    7                  11            14  15   x
			//                        /                  /sq/|/          /        /|   /
			//                       /                  /--/------------/        / |  / 11
			//                      /                  /               / square / /  /
			//                     /     square-6     /               /        / /  / 10
			//                    /                  /               /        / /  /
			//                   /                  /               /--------/--| /
			//                  /                  /   square-5    /           /|/
			//                 /                  /               /           / | 8
			//                /                  /               /           / /
			//               /------------------/               / square-4  / /
			//              /                  /               /           / /
			//             /                  /---------------/           / / 6
			//            /                  /               /           / /
			//           /                  /               /-----------| / 5
			//          /                  /               /           /|/
			//         /                  /               /           / | 4
			//        /                  /               /           / /
			//   z   /                  /               /           / /
			//      /                  /               /           / /
			//   | /                  /               /           / /
			//   |/                  /               /           / /
			// 1 |------------------|---------------|-----------| /
			//   |     square-6     |   square-5    | square-4  |/
			// 0 |------------------|---------------|-----------|-- x
			//   0                  6              11          15
			//
			// y                                                                                z
			// 12 +---------------------------+---+                                             1 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                                               |       square-4       |       square-4        |    square-3     | sq |
			//    |                           |   |                                               |                      |                       |                 |    |
			// 11 |                           +---+                  +-------------+            0 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                  |             |              0                      4     5     6           8           10    11   12   y
			// 10 |                           +---+------------------+             |
			//    |                           |                      |  square-3   |
			//    |         square-6          |                      |             |
			//    |                           |                      |             |
			//  8 |                           |                      +-------------+---+
			//    |                           |       square-5       |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  6 +---------------------------+                      |    square-4     |
			//    |                           |                      |                 |
			//  5 |                           +----------------------+                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  4 |                           |                      +-----------------+
			//    |                           |                      |                 |
			//    |         square-6          |                      |                 |
			//    |                           |       square-5       |                 |
			//    |                           |                      |    square-4     |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  0 +---------------------------+----------------------+-----------------+
			//    0                           6   7                  11            14  15   x
			// </figure>
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
		assertThat(costly.getAttempts()).isEqualTo(1);
	}

	@Test
	public void budgetLimitsTheCostlyPackager() throws Exception {
		// brute force (not the fast variant) takes far longer than the budget to find the exact arrangement
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(BruteForcePackager.newBuilder().build(), 1)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterruptDuration(INTERRUPT_DURATION).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+-----------------------+-----------------+
			//                           /------------------/--|                          /       |         square-6          |       square-5        |    square-4     |
			//                          /                  /sq/|                         /      0 +---------------------------+-----------------------+-----------------+
			//                         /                  /--| |          /--------|    / 12      0                           6    7                  11            14  15   x
			//                        /                  /sq/|/          /        /|   /
			//                       /                  /--/------------/        / |  / 11
			//                      /                  /               / square / /  /
			//                     /     square-6     /               /        / /  / 10
			//                    /                  /               /        / /  /
			//                   /                  /               /--------/--| /
			//                  /                  /   square-5    /           /|/
			//                 /                  /               /           / | 8
			//                /                  /               /           / /
			//               /------------------/               / square-4  / /
			//              /                  /               /           / /
			//             /                  /---------------/           / / 6
			//            /                  /               /           / /
			//           /                  /               /-----------| / 5
			//          /                  /               /           /|/
			//         /                  /               /           / | 4
			//        /                  /               /           / /
			//   z   /                  /               /           / /
			//      /                  /               /           / /
			//   | /                  /               /           / /
			//   |/                  /               /           / /
			// 1 |------------------|---------------|-----------| /
			//   |     square-6     |   square-5    | square-4  |/
			// 0 |------------------|---------------|-----------|-- x
			//   0                  6              11          15
			//
			// y                                                                                z
			// 12 +---------------------------+---+                                             1 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                                               |       square-4       |       square-4        |    square-3     | sq |
			//    |                           |   |                                               |                      |                       |                 |    |
			// 11 |                           +---+                  +-------------+            0 +----------------------+-----------------------+-----------------+----+
			//    |                           | s |                  |             |              0                      4     5     6           8           10    11   12   y
			// 10 |                           +---+------------------+             |
			//    |                           |                      |  square-3   |
			//    |         square-6          |                      |             |
			//    |                           |                      |             |
			//  8 |                           |                      +-------------+---+
			//    |                           |       square-5       |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  6 +---------------------------+                      |    square-4     |
			//    |                           |                      |                 |
			//  5 |                           +----------------------+                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  4 |                           |                      +-----------------+
			//    |                           |                      |                 |
			//    |         square-6          |                      |                 |
			//    |                           |       square-5       |                 |
			//    |                           |                      |    square-4     |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//    |                           |                      |                 |
			//  0 +---------------------------+----------------------+-----------------+
			//    0                           6   7                  11            14  15   x
			// </figure>
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
	}

	@Test
	public void acceptedResultsKeepThePackagersInSync() throws Exception {
		// The squares and one more 1 x 1 box: brute force fills the exact container, plain packs the extra box
		//
		//   exact: 6 6 5 5 4 4 3 1 1      single: 1
		//
		List<ContainerItem> containers = List.of(new ContainerItem(container("exact", 15, 11), 1), new ContainerItem(container("single", 1, 1), 1));
		List<BoxItem> boxItems = new ArrayList<>(squares());
		boxItems.add(new BoxItem(Box.newBuilder().withId("extra").withSize(1, 1, 1).withWeight(1).build(), 1));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();
			// <figure>
			// container 1 of 2: exact
			//                                                                              y   z
			//                                                                                  1 +---------------------------+------------------+----------------------+
			//                         /---------------/-------------/------------------| /       |         square-6          |     square-4     |       square-5       |
			//                        /               /             /                  /|/      0 +---------------------------+------------------+----------------------+
			//                       /               /             /                  / | 11      0                      5    6             9    10                     15   x
			//                      /               /             /                  / /
			//                     /               /  square-4   /                  / /
			//                    /   square-5    /             /                  / /
			//                   /               /             /     square-6     / /
			//                  /               /             /                  / /
			//                 /               /---/---------/                  / /
			//                /               / s /         /                  / /
			//               /---------------/---/         /                  / / 7
			//              /                   / square- /                  / /
			//             /                   /         /--/---------------| / 6
			//            /                   /         /sq/               /|/
			//           /                   /---------/--/               / | 5
			//          /                   /            /               / /
			//         /                   /            /               / / 4
			//        /                   /            /               / /
			//   z   /                   /            /               / /
			//      /                   /            /               / /
			//   | /                   /            /               / /
			//   |/                   /            /               / /
			// 1 |-------------------|------------|---------------| /
			//   |     square-6      |  square-4  |   square-5    |/
			// 0 |-------------------|------------|---------------|-- x
			//   0                   6           10              15
			//
			// y                                                                                z
			// 11 +----------------------+------------------+--------------------------+        1 +-------------------------------+-------------------------------------+
			//    |                      |                  |                          |          |           square-5            |              square-6               |
			//    |                      |                  |                          |          |                               |                                     |
			//    |                      |                  |                          |        0 +-------------------------------+-------------------------------------+
			//    |                      |     square-4     |                          |          0                         4     5     6      7                        11   y
			//    |       square-5       |                  |                          |
			//    |                      |                  |         square-6         |
			//    |                      |                  |                          |
			//    |                      |                  |                          |
			//  7 |                      +----+-------------+                          |
			//    |                      | sq |             |                          |
			//  6 +----------------------+----+             |                          |
			//    |                           |  square-3   |                          |
			//  5 |                           |             +---+----------------------+
			//    |                           |             | s |                      |
			//    |                           |             |   |                      |
			//  4 |                           +-------------+---+                      |
			//    |                           |                 |                      |
			//    |         square-6          |                 |                      |
			//    |                           |                 |       square-5       |
			//    |                           |    square-4     |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//  0 +---------------------------+-----------------+----------------------+
			//    0                      5    6             9   10                     15   x
			//
			// container 2 of 2: single
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         | extra |         | extra |         | extra |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   | extra | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactlyInAnyOrder("exact", "single");
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	@Test
	public void packsBoxItemGroups() throws Exception {
		//  first: [a][a]   second: [b][b]
		List<ContainerItem> containers = List.of(new ContainerItem(container("row", 2, 1), 2));
		List<BoxItemGroup> groups = List.of(
				new BoxItemGroup("first", List.of(new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 2))),
				new BoxItemGroup("second", List.of(new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 2))));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItemGroups(groups).withMaxContainerCount(2).build();
			// <figure>
			// container 1 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   a   |         |   a   |   a   |         |   a   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   a   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   b   |   b   |         |   b   |   b   |         |   b   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   b   |   b   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			// </figure>
			figure(result);

			assertThat(result.size()).isEqualTo(2);
			for(Container container : result.getContainers()) {
				assertThat(container.getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsOnly(container.getStack().getPlacements().get(0).getStackValue().getBox().getId());
			}
		}
	}

	@Test
	public void packsBoxItemGroupsWhichFitOneContainerType() throws Exception {
		// The first group fits only the big container. Plain may pack the groups out of order, which brute force
		// does not, but brute force continues from such results.
		//
		//   big: [long  ]      small: [c]
		//
		List<ContainerItem> containers = List.of(new ContainerItem(container("small", 1, 1), 1), new ContainerItem(container("big", 2, 1), 1));
		List<BoxItemGroup> groups = List.of(
				new BoxItemGroup("long", List.of(new BoxItem(Box.newBuilder().withId("long").withSize(2, 1, 1).withWeight(1).build(), 1))),
				new BoxItemGroup("cube", List.of(new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 1))));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItemGroups(groups).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
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

			assertThat(result.getContainers())
					.extracting(c -> c.getId() + ":" + c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
					.containsExactlyInAnyOrder("big:long", "small:cube");
		}
	}

	@Test
	public void skipsPackagersWhichDoNotSupportTheInput() throws Exception {
		// brute force does not support custom point controls
		ContainerItem containerItem = new ContainerItem(container("row", 2, 1), 1);
		containerItem.setPointControlsBuilderFactory(HeavyItemsOnGroundLevelPointControls.newFactory(100));
		List<ContainerItem> containers = List.of(containerItem);
		List<BoxItem> boxItems = List.of(new BoxItem(square(1), 2));
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();
			// <figure>
			//   z                               z                               y                               z
			//                                   1 +----------+----------+       1 +----------+----------+       1 +----------+
			//   | /----------/----------|   y     |          |          |         |          |          |         |          |
			//   |/          /          /|         | square-1 | square-1 |         | square-1 | square-1 |         | square-1 |
			// 1 |----------|----------| | /       |          |          |         |          |          |         |          |
			//   |          |          | |/      0 +----------+----------+       0 +----------+----------+       0 +----------+
			//   | square-1 | square-1 | | 1       0          1          2   x     0          1          2   x     0          1   y
			//   |          |          |/
			// 0 |----------|----------|-- x
			//   0          1          2
			// </figure>
			figure(result);

			assertThat(result.isSuccess()).isTrue();
		}
		assertThat(costly.getAttempts()).isZero();
	}

	//
	//  two cubes; a small container holds one (cost 10, two available), the large container both (cost 100).
	//  The cheapest packing uses the small containers:
	//
	//    small [a]   small [b]      not      large [a][b]
	//
	@Test
	public void usesTheCheapestContainers() throws Exception {
		Container small = Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		Container large = Container.newBuilder().withId("large").withSize(2, 1, 1).withMaxLoadWeight(2).build();
		List<ContainerItem> containers = List.of(
				new ContainerItem(small, 2, new FixedContainerCostCalculator(10, small.getVolume(), null, 0)),
				new ContainerItem(large, 1, new FixedContainerCostCalculator(100, large.getVolume(), null, 0)));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build(), 1000)
				.build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(new BoxItem(square(1), 2))
					.withMaxContainerCount(2)
					.withInterruptDuration(INTERRUPT_DURATION)
					.build();
			// <figure>
			// container 1 of 2: small
			//   z                    z                    y                    z
			//                        1 +----------+       1 +----------+       1 +----------+
			//   | /----------|   y     |          |         |          |         |          |
			//   |/          /|         | square-1 |         | square-1 |         | square-1 |
			// 1 |----------| | /       |          |         |          |         |          |
			//   |          | |/      0 +----------+       0 +----------+       0 +----------+
			//   | square-1 | | 1       0          1   x     0          1   x     0          1   y
			//   |          |/
			// 0 |----------|-- x
			//   0          1
			//
			// container 2 of 2: small
			//   z                    z                    y                    z
			//                        1 +----------+       1 +----------+       1 +----------+
			//   | /----------|   y     |          |         |          |         |          |
			//   |/          /|         | square-1 |         | square-1 |         | square-1 |
			// 1 |----------| | /       |          |         |          |         |          |
			//   |          | |/      0 +----------+       0 +----------+       0 +----------+
			//   | square-1 | | 1       0          1   x     0          1   x     0          1   y
			//   |          |/
			// 0 |----------|-- x
			//   0          1
			// </figure>
			figure(result);

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("small", "small");
			assertThat(result.getCost()).isEqualTo(20);
		}
	}

	@Test
	public void usesAtMostAsManyContainersAsTheBaseline() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();
			// <figure>
			//                                                                              y   z
			//                                                                                  1 +---------------------------+------------------+----------------------+
			//                         /---------------/-------------/------------------| /       |         square-6          |     square-4     |       square-5       |
			//                        /               /             /                  /|/      0 +---------------------------+------------------+----------------------+
			//                       /               /             /                  / | 11      0                      5    6             9    10                     15   x
			//                      /               /             /                  / /
			//                     /               /  square-4   /                  / /
			//                    /   square-5    /             /                  / /
			//                   /               /             /     square-6     / /
			//                  /               /             /                  / /
			//                 /               /---/---------/                  / /
			//                /               / s /         /                  / /
			//               /---------------/---/         /                  / / 7
			//              /                   / square- /                  / /
			//             /                   /         /--/---------------| / 6
			//            /                   /         /sq/               /|/
			//           /                   /---------/--/               / | 5
			//          /                   /            /               / /
			//         /                   /            /               / / 4
			//        /                   /            /               / /
			//   z   /                   /            /               / /
			//      /                   /            /               / /
			//   | /                   /            /               / /
			//   |/                   /            /               / /
			// 1 |-------------------|------------|---------------| /
			//   |     square-6      |  square-4  |   square-5    |/
			// 0 |-------------------|------------|---------------|-- x
			//   0                   6           10              15
			//
			// y                                                                                z
			// 11 +----------------------+------------------+--------------------------+        1 +-------------------------------+-------------------------------------+
			//    |                      |                  |                          |          |           square-5            |              square-6               |
			//    |                      |                  |                          |          |                               |                                     |
			//    |                      |                  |                          |        0 +-------------------------------+-------------------------------------+
			//    |                      |     square-4     |                          |          0                         4     5     6      7                        11   y
			//    |       square-5       |                  |                          |
			//    |                      |                  |         square-6         |
			//    |                      |                  |                          |
			//    |                      |                  |                          |
			//  7 |                      +----+-------------+                          |
			//    |                      | sq |             |                          |
			//  6 +----------------------+----+             |                          |
			//    |                           |  square-3   |                          |
			//  5 |                           |             +---+----------------------+
			//    |                           |             | s |                      |
			//    |                           |             |   |                      |
			//  4 |                           +-------------+---+                      |
			//    |                           |                 |                      |
			//    |         square-6          |                 |                      |
			//    |                           |                 |       square-5       |
			//    |                           |    square-4     |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//    |                           |                 |                      |
			//  0 +---------------------------+-----------------+----------------------+
			//    0                      5    6             9   10                     15   x
			// </figure>
			figure(result);

			assertThat(result.size()).isEqualTo(1);
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	private static List<ContainerItem> exactAndLargerContainers() {
		return List.of(new ContainerItem(container("exact", 15, 11), 1), new ContainerItem(container("larger", 16, 12), 1));
	}

	/** The squares of the Bouwkamp code 15x11A. */
	private static List<BoxItem> squares() {
		return List.of(
				new BoxItem(square(6), 2),
				new BoxItem(square(5), 2),
				new BoxItem(square(4), 2),
				new BoxItem(square(3), 1),
				new BoxItem(square(1), 2));
	}

	private static Box square(int size) {
		return Box.newBuilder().withId("square-" + size).withSize(size, size, 1).withRotate3D().withWeight(1).build();
	}

	private static Container container(String id, int dx, int dy) {
		return Container.newBuilder().withId(id).withSize(dx, dy, 1).withMaxLoadWeight(100).build();
	}
}
