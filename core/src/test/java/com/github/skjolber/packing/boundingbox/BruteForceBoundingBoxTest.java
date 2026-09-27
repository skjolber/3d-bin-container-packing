package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

/*
 * Diagram conventions: schematic views, with the viewing plane and dimensions
 * stated per test. Gaps belong to the rectangular envelope, not to box volume.
 * Labels distinguish illustrative candidates, rejected layouts and asserted results.
 */
class BruteForceBoundingBoxTest {

	/*
	 * Six rotating 2 x 1 x 1 bars can fill this 6 x 2 x 1 floor:
	 *
	 *       +---------+---------+---------+
	 *       |    A    |    A    |    A    |
	 *       +---------+---------+---------+
	 *       |    A    |    A    |    A    |
	 *       +---------+---------+---------+
	 *
	 * Other rotations change the remaining minimum area. Applying the next
	 * box's minimum before consuming the selected point used to remove/reindex
	 * that point and crash during exhaustive traversal.
	 */
	@Test
	void consumesSelectedPointBeforeFilteringForTheNextRotation() {
		BoxItem bars = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 6);
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(bars).withContainer(container(6, 3, 3)).build();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertThat(result.getObjectiveResults().get("default").getBoundingBox().getVolume()).isEqualTo(12);
			assertLayout(result, 6, 12);
		}
	}

	/*
	 * A standalone search owns its deadline scheduler; no packager is needed.
	 * One unit cube fits inside a 2 x 2 x 2 container.
	 *
	 *     Container, side view (depth 2)
	 *     +-------------------------------+
	 *     |                               |
	 *     |           unused              |
	 *     |                               |
	 *     +---------------+               |
	 *     |               |               |
	 *     |   unit cube   |    unused     |
	 *     |               |               |
	 *     +---------------+---------------+
	 *
	 *     Start ----> Complete search --------------------> Deadline
	 *                       |                              +60 seconds
	 *                       v
	 *                EXHAUSTED, success
	 *
	 * The tiny search finishes before the future deadline and returns the cube.
	 */
	@Test
	void supportsFutureDeadlinesWithoutAPackager() {
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(item(1, 1, 1)).withContainer(container(2, 2, 2))
					.withInterruptDuration(60_000).build();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertLayout(result, 1, 1);
		}
	}

	/*
	 * The reusable component may create operation builders only while open.
	 *
	 *     +-------------------------+
	 *     |                         |
	 *     |  BruteForceBoundingBox  |
	 *     |          OPEN           |
	 *     |                         |
	 *     +-------------------------+
	 *                  |
	 *                  | close()
	 *                  v
	 *     +-------------------------+
	 *     |                         |
	 *     |         CLOSED          |
	 *     |   scheduler released    |
	 *     |                         |
	 *     +-------------------------+
	 *                  |
	 *                  | newResultBuilder()
	 *                  v
	 *        IllegalStateException
	 *
	 * No new operation may silently reuse the closed component.
	 */
	@Test
	void rejectsNewOperationsAfterClose() {
		BruteForceBoundingBox boundingBox = new BruteForceBoundingBox();
		boundingBox.close();
		assertThatThrownBy(boundingBox::newResultBuilder)
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
	}

	/*
	 * A = 3 x 2 x 1; B = 2 x 1 x 1, with permitted in-plane rotations.
	 * The search container is 5 x 4 x 3. Representative top views:
	 *
	 *     A LOOSER COMPLETE LAYOUT            OPTIMAL FILLED LAYOUT
	 *
	 *     +---------+-------------------+
	 *     |         |                   |
	 *     |    B    |       gap         |
	 *     | rotated |                   |
	 *     |         |                   |
	 *     +---------+-------------------+    +-----------------------------+---------+
	 *     |                             |    |                             |         |
	 *     |                             |    |                             |    B    |
	 *     |              A              |    |              A              | rotated |
	 *     |                             |    |                             |         |
	 *     +-----------------------------+    +-----------------------------+---------+
	 *
	 *     Envelope: 3 x 4 x 1                Envelope: 4 x 2 x 1
	 *     Volume: 12                         Volume: 8
	 *
	 * The left layout illustrates wasted envelope space, not an asserted first
	 * placement order. The test requires the first complete result to be larger
	 * than the optimum, and the exhaustive result to be exactly 4 x 2 x 1.
	 * Running the exhaustive operation must not mutate the earlier snapshot.
	 */
	@Test
	void improvesBoundingVolumeAfterTheFirstCompletePacking() {
		List<BoxItem> items = List.of(item(3, 2, 1), item(2, 1, 1));
		Container container = container(5, 4, 3);
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult first = boundingBox.newResultBuilder()
					.withBoxItems(items).withContainer(container).withGoal(bounds -> true).build();
			BruteForceBoundingBoxResult best = boundingBox.newResultBuilder()
					.withBoxItems(items).withContainer(container).build();

			assertThat(first.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertThat(best.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertThat(best.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(4, 2, 1));
			assertThat(best.getObjectiveResults().get("default").getBoundingBox().getVolume()).isLessThan(first.getObjectiveResults().get("default").getBoundingBox().getVolume());
			assertLayout(best, 2, 8);
			assertLayout(first, 2, 8); // A later operation must not mutate the first result.
		}
	}

	/*
	 * A unit cube and a 2 x 1 x 1 box fit inside a 3 x 3 x 3 container.
	 *
	 *     TWO ORDERS
	 *
	 *        Cube --> Long box
	 *
	 *        Long box --> Cube
	 *
	 *
	 *     THREE LONG-BOX ORIENTATIONS
	 *
	 *        Along X                       Along Z
	 *        +-----------------------+     +-----------+
	 *        |                       |     |           |
	 *        |       LONG BOX        |     |           |
	 *        |                       |     | LONG BOX  |
	 *        +-----------------------+     |           |
	 *                                      |           |
	 *        Along Y                       +-----------+
	 *        Extends into the page:
	 *        dimensions 1 x 2 x 1
	 *
	 *
	 *     THREE POINT CHOICES FOR THE SECOND BOX
	 *
	 *                     Above
	 *                       |
	 *                       v
	 *
	 *                   +-------+
	 *                   | FIRST |
	 *                   +-------+ ----> Beside
	 *
	 *                       \
	 *                        \----> Behind
	 *
	 * The goal always returns false, so it observes every complete candidate:
	 * 2 orders x 3 rotations x 3 points = 18. The result is EXHAUSTED.
	 */
	@Test
	void enumeratesEveryPermutationRotationAndFittingPointUntilGoal() {
		BoxItem unit = item(1, 1, 1);
		BoxItem rotating = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build());
		AtomicInteger layouts = new AtomicInteger();
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(unit, rotating).withContainer(container(3, 3, 3))
					.withGoal(bounds -> {
						layouts.incrementAndGet();
						return false;
					}).build();

			// Two permutations, every orientation, and three positions for the second box.
			assertThat(layouts.get()).isEqualTo(2 * rotating.getBox().getStackValues().length * 3);
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertLayout(result, 2, 3);
		}
	}

	/*
	 * Four copies of a 2 x 1 x 1 box have total volume 8.
	 * The goal requires volume 8, width 4 and height 1, inside a 4 x 4 x 4 container.
	 * Top view of a qualifying assembly (labels distinguish physical copies):
	 *
	 *                  <------------ width 4 ------------>
	 *
	 *                  +-------------------+-------------------+
	 *                  |                   |                   |
	 *                  |        C          |        D          |
	 *                  |                   |                   |
	 *                  +-------------------+-------------------+  depth 2
	 *                  |                   |                   |
	 *                  |        A          |        B          |
	 *                  |                   |                   |
	 *                  +-------------------+-------------------+
	 *
	 *                  Height = 1
	 *                  Envelope volume = 4 x 2 x 1 = 8
	 *                  Sum of box volumes = 8: no gaps
	 *
	 * Earlier complete layouts do not satisfy this exact shape goal.
	 * Stop when a qualifying layout is found: GOAL_REACHED.
	 */
	@Test
	void reachesAFilledRectangularAssemblyGoal() {
		BoxItem repeated = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).build(), 4);
		AtomicInteger visited = new AtomicInteger();
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(repeated).withContainer(container(4, 4, 4))
					.withGoal(bounds -> {
						visited.incrementAndGet();
						return bounds.getVolume() == 8 && bounds.dx() == 4 && bounds.dz() == 1;
					}).build();

			assertThat(visited.get()).isGreaterThan(1);
			assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(4, 2, 1));
			assertLayout(result, 4, 8);
		}
	}

	/*
	 * A = 3 x 2 x 1; B = 2 x 1 x 1.
	 * The custom comparator prefers LARGER volume, but the goal requires volume 9.
	 * Representative top views, all with height 1:
	 *
	 *     COMPARATOR PREFERS THIS             GOAL ACCEPTS THIS
	 *
	 *     +---------+-------------------+
	 *     |         |                   |
	 *     |    B    |       gap         |
	 *     | rotated |                   |
	 *     |         |                   |
	 *     +---------+-------------------+     +-------------------+---------+
	 *     |                             |     |         B         |   gap   |
	 *     |                             |     +-------------------+---------+
	 *     |              A              |     |                             |
	 *     |                             |     |              A              |
	 *     +-----------------------------+     |                             |
	 *                                         +-----------------------------+
	 *
	 *     Envelope volume = 12                Envelope volume = 9
	 *
	 * A goal is not merely a filter on comparator improvements:
	 * return the first goal-satisfying complete layout, even when a previous
	 * candidate ranked better. Termination must be GOAL_REACHED.
	 */
	@Test
	void acceptsGoalEvenWhenItIsWorseThanAnEarlierLayout() {
		AtomicInteger visited = new AtomicInteger();
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(item(3, 2, 1), item(2, 1, 1)).withContainer(container(5, 4, 3))
					.withComparator((left, right) -> Long.compare(right.getVolume(), left.getVolume()))
					.withGoal(bounds -> {
						visited.incrementAndGet();
						return bounds.getVolume() == 9;
					}).build();

			assertThat(visited.get()).isGreaterThan(1);
			assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertThat(result.getObjectiveResults().get("default").getBoundingBox().getVolume()).isEqualTo(9);
			assertLayout(result, 2, 8);
		}
	}

	/*
	 * The custom comparator deliberately MAXIMIZES envelope volume.
	 * A = 3 x 2 x 1; B = 2 x 1 x 1. Representative top views:
	 *
	 *     SMALLER ENVELOPE                    LARGER ENVELOPE
	 *
	 *                                         +---------+-------------------+
	 *                                         |         |                   |
	 *                                         |    B    |       gap         |
	 *                                         | rotated |                   |
	 *                                         |         |                   |
	 *     +-------------------+---------+     +---------+-------------------+
	 *     |                   |         |     |                             |
	 *     |         A         |    B    |     |              A              |
	 *     |                   | rotated |     |                             |
	 *     +-------------------+---------+     +-----------------------------+
	 *
	 *     Dimensions: 4 x 2 x 1                Dimensions: 3 x 4 x 1
	 *     Volume: 8                           Volume: 12
	 *
	 * The left drawing is schematic: A occupies width 3 and B width 1.
	 * Default minimum-volume pruning would be wrong for this comparator.
	 * Search to EXHAUSTED and return volume 12; a particular tied shape is not required.
	 */
	@Test
	void supportsCustomObjectivesWithoutVolumePruning() {
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(item(3, 2, 1), item(2, 1, 1)).withContainer(container(5, 4, 3))
					.withComparator((left, right) -> Long.compare(right.getVolume(), left.getVolume())).build();

			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertThat(result.getObjectiveResults().get("default").getBoundingBox().getVolume()).isEqualTo(12);
			assertLayout(result, 2, 8);
		}
	}

	/*
	 * Two boxes each measure 2 x 2 x 1. Side views; both have depth 2:
	 *
	 *     SIDE BY SIDE
	 *
	 *     +-------------------+-------------------+
	 *     |                   |                   |
	 *     |         A         |         B         |
	 *     |                   |                   |
	 *     +-------------------+-------------------+
	 *
	 *     Dimensions: 4 x 2 x 1
	 *     Volume = 8; surface area = 28
	 *
	 *
	 *     STACKED
	 *
	 *     +-------------------+
	 *     |                   |
	 *     |         B         |
	 *     |                   |
	 *     +-------------------+
	 *     |                   |
	 *     |         A         |
	 *     |                   |
	 *     +-------------------+
	 *
	 *     Dimensions: 2 x 2 x 2
	 *     Volume = 8; surface area = 24
	 *
	 * With equal volume, the smaller surface area wins: return the 2 x 2 x 2 cube.
	 */
	@Test
	void prefersCompactSurfaceAreaAmongEqualVolumeLayouts() {
		BoxItem repeated = new BoxItem(Box.newBuilder().withSize(2, 2, 1).withWeight(1).build(), 2);
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(repeated).withContainer(container(4, 4, 4)).build();
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(2, 2, 2));
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertLayout(result, 2, 8);
		}
	}

	/*
	 * Two slabs each measure 2000 x 2000 x 1000.
	 * Side view, with depth 2000:
	 *
	 *                  <-------- width 2000 -------->
	 *
	 *                  +---------------------------+
	 *                  |                           |
	 *                  |        upper slab         |  height 1000
	 *                  |                           |
	 *                  +---------------------------+
	 *                  |                           |
	 *                  |        lower slab         |  height 1000
	 *                  |                           |
	 *                  +---------------------------+
	 *
	 *                  Total height = 2000
	 *                  Envelope = 2000 x 2000 x 2000
	 *                  Volume = 8,000,000,000
	 *
	 * The 4000 x 4000 x 4000 search container also exceeds int volume range.
	 * Extent comparisons and returned volumes must use long arithmetic.
	 */
	@Test
	void handlesVolumesLargerThanIntegerRange() {
		BoxItem repeated = new BoxItem(Box.newBuilder().withSize(2000, 2000, 1000).withWeight(1).build(), 2);
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(repeated).withContainer(container(4000, 4000, 4000)).build();
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(2000, 2000, 2000));
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertLayout(result, 2, 8_000_000_000L);
		}
	}

	/*
	 * Two boxes have total volume 8. The callback interrupts the search after
	 * the first complete candidate, but returns false rather than accepting a goal.
	 *
	 *     Place first box
	 *            |
	 *            v
	 *     Place second box
	 *            |
	 *            v
	 *     +-----------------------------------+
	 *     |      COMPLETE CANDIDATE FOUND     |
	 *     |                                   |
	 *     |  Goal callback sets stop = true   |
	 *     |  Goal callback returns false      |
	 *     +-----------------------------------+
	 *            |
	 *            v
	 *     Save the complete candidate
	 *            |
	 *            v
	 *     Next interrupt check observes stop
	 *            |
	 *            v
	 *     +-----------------------------------+
	 *     |            INTERRUPTED            |
	 *     |                                   |
	 *     |  Return both boxes, not a prefix  |
	 *     |  Packed box volume remains 8     |
	 *     +-----------------------------------+
	 *
	 * A timeout/interrupt is not equivalent to having found no solution.
	 */
	@Test
	void retainsTheBestCompleteLayoutWhenInterrupted() {
		AtomicBoolean stop = new AtomicBoolean();
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(item(3, 2, 1), item(2, 1, 1)).withContainer(container(5, 4, 3))
					.withGoal(bounds -> {
						stop.set(true);
						return false;
					}).withInterrupt(stop::get).build();

			assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertLayout(result, 2, 8);
		}
	}

	/*
	 * One unit cube could fit in the 2 x 2 x 2 container, but the deadline
	 * is already in the past.
	 *
	 *     Past deadline                Build operation
	 *           |                            |
	 *           v                            v
	 *     ------+----------------------------+----------------> time
	 *                                        |
	 *                                        v
	 *                             Interrupt before searching
	 *                                        |
	 *                                        v
	 *                             +-------------------------+
	 *                             |      INTERRUPTED        |
	 *                             |                         |
	 *                             |  success = false        |
	 *                             |  empty stack            |
	 *                             |  bounding box = null    |
	 *                             +-------------------------+
	 *
	 * Do not return a fabricated placement just because the input is trivial.
	 */
	@Test
	void expiredDeadlineReturnsNoLayoutWithoutSearching() {
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(item(1, 1, 1)).withContainer(container(2, 2, 2)).withInterruptDeadline(0).build();
			assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertThat(result.isSuccess()).isFalse();
			assertThat(result.getObjectiveResults()).isEmpty();
			assertThat(result.getResults()).isEmpty();
		}
	}

	/*
	 * Every supplied box must fit. Keeping just the first box is not success.
	 *
	 *     VOLUME FAILURE: 2 x 2 x 1 container, top view
	 *
	 *     +-----------------------+        +-----------+
	 *     |                       |        |           |
	 *     |       2 x 2 box       |        | unit box  |  cannot also fit
	 *     |                       |        |           |
	 *     +-----------------------+        +-----------+
	 *
	 *     Total box volume = 5; container volume = 4.
	 *
	 *
	 *     DIMENSION FAILURE: 1 x 4 x 1 container, top view
	 *
	 *     +-----------+                    +-----------------------+
	 *     |           |                    |                       |
	 *     |           |                    |       2 x 2 box       |
	 *     | width = 1 |                    |                       |
	 *     |           |                    +-----------------------+
	 *     |           |
	 *     |           |                    Both in-plane orientations
	 *     +-----------+                    are too wide.
	 *
	 *
	 *     WEIGHT FAILURE
	 *
	 *     Two boxes weighing 1 each --> total weight 2
	 *     Container max load weight --> 1
	 *     Geometry fits, but weight does not.
	 *
	 *
	 *     GEOMETRY FAILURE DESPITE SUFFICIENT TOTAL VOLUME
	 *
	 *     3 x 3 x 1 container, top view
	 *     +-----------------------+-----------+
	 *     |                       |           |
	 *     |                       |           |
	 *     |       first 2 x 2     |  width 1  |
	 *     |                       |           |
	 *     +-----------------------+-----------+
	 *     |          remaining strip          |
	 *     +-----------------------------------+
	 *
	 *     Two 2 x 2 boxes have volume 8; container volume is 9.
	 *     The remaining strips cannot accommodate the second 2 x 2 box.
	 *
	 * All cases fail without returning a partial assembly. The goal must never
	 * be invoked for the first three infeasible inputs.
	 */
	@Test
	void neverReturnsAPartialAssembly() {
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			for(Container container : List.of(container(2, 2, 1), container(1, 4, 1),
					Container.newBuilder().withSize(4, 4, 4).withMaxLoadWeight(1).build())) {
				BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
						.withBoxItems(item(2, 2, 1), item(1, 1, 1)).withContainer(container)
						.withGoal(bounds -> { throw new AssertionError("No complete layout can exist"); }).build();
				assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
				assertThat(result.isSuccess()).isFalse();
				assertThat(result.getObjectiveResults()).isEmpty();
				assertThat(result.getResults()).isEmpty();
			}
			// Volume and weight fit, but two 2x2 squares cannot fit in a 3x3 footprint.
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(new BoxItem(Box.newBuilder().withSize(2, 2, 1).withWeight(1).build(), 2))
					.withContainer(container(3, 3, 1)).build();
			assertThat(result.isSuccess()).isFalse();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
		}
	}

	/*
	 * The outside container is 9 x 9 x 9, but usable load space is only 1 x 2 x 2.
	 * Two 2 x 1 x 1 boxes must rotate to fit that usable space.
	 * Schematic Y-Z view; usable width X is 1:
	 *
	 *     +---------------------------------------------------+
	 *     |             OUTSIDE CONTAINER: 9 x 9 x 9           |
	 *     |                                                   |
	 *     |                                                   |
	 *     |    USABLE SPACE                                   |
	 *     |    +-----------------------+                      |
	 *     |    |                       |                      |
	 *     |    |     rotated copy      |                      |
	 *     |    |                       |                      |
	 *     |    +-----------------------+  load height 2       |
	 *     |    |                       |                      |
	 *     |    |     rotated copy      |                      |
	 *     |    |                       |                      |
	 *     |    +-----------------------+                      |
	 *     |       load depth 2                                |
	 *     +---------------------------------------------------+
	 *
	 * The illustrated arrangement is one way to occupy the required 1 x 2 x 2 bounds.
	 * Returned placements retain the original BoxItem and original stack values.
	 * Input count 2, local index 17, global index 41 and box backlink remain unchanged.
	 * The input container's stack remains empty.
	 */
	@Test
	void respectsLoadDimensionsAndPreservesInputIdentitiesAndOrientationIndexes() {
		BoxItem original = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 2, 17, 41);
		Container container = Container.newBuilder().withSize(9, 9, 9).withLoadSize(1, 2, 2).withMaxLoadWeight(10).build();
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
					.withBoxItems(original).withContainer(container).build();
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(1, 2, 2));
			assertLayout(result, 2, 4);
			for(Placement placement : result.getObjectiveResults().get("default").getStack()) {
				assertThat(placement.getBoxItem()).isSameAs(original);
				assertThat(placement.getStackValue()).isIn((Object[]) original.getBox().getStackValues());
			}
			assertThat(original.getCount()).isEqualTo(2);
			assertThat(original.getLocalIndex()).isEqualTo(17);
			assertThat(original.getGlobalIndex()).isEqualTo(41);
			assertThat(original.getBox().getBoxItem()).isSameAs(original);
			assertThat(container.getStack().isEmpty()).isTrue();
		}
	}

	/*
	 * Two unit cubes each declare maximum load 0.
	 *
	 *     +-------------------+
	 *     |                   |
	 *     |     unit cube     |
	 *     |                   |
	 *     +-------------------+
	 *     |                   |
	 *     |     unit cube     |    A geometric-only search must not
	 *     |     limit = 0     |    silently ignore this constraint.
	 *     +-------------------+
	 *     =====================
	 *
	 * The 2 x 2 x 2 container could also fit the boxes side by side, but that
	 * does not make it safe to ignore their declared constraints.
	 * BruteForceBoundingBox rejects the constrained input with IllegalArgumentException.
	 * Use LoadBruteForceBoundingBox when these constraints must be evaluated.
	 */
	@Test
	void rejectsLoadConstraintsRatherThanIgnoringThem() {
		BoxItem constrained = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(0).build(), 2);
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			assertThatThrownBy(() -> boundingBox.newResultBuilder()
					.withBoxItems(constrained).withContainer(container(2, 2, 2)).build())
					.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("load constraints");
		}
	}

	/*
	 * Four small inputs are checked in a 3 x 3 x 2 container:
	 *   - 2 x 2 x 1, 2 x 1 x 1 and one unit cube;
	 *   - 3 x 1 x 1 and 2 x 2 x 1;
	 *   - three 2 x 1 x 1 boxes with full 3D rotation;
	 *   - three 2 x 2 x 1 boxes.
	 *
	 *                        SAME INPUT
	 *                            |
	 *           +----------------+----------------+
	 *           |                |                |
	 *           v                v                v
	 *     +-------------+  +-------------+  +-----------------------+
	 *     | Brute force |  | Brute force |  | Independent reference |
	 *     |             |  |             |  |                       |
	 *     | Safe volume |  | Goal always |  | Every integer position|
	 *     | pruning     |  | false: no   |  | Every allowed rotation|
	 *     | enabled     |  | goal stop  |  | Collision checks      |
	 *     |             |  | or volume  |  | No point calculator   |
	 *     |             |  | pruning   |  |                       |
	 *     +-------------+  +-------------+  +-----------------------+
	 *           |                |                |
	 *           v                v                v
	 *        Optimum          Optimum          Optimum
	 *           |                |                |
	 *           +----------------+----------------+
	 *                            |
	 *                            v
	 *                     Must all agree
	 *
	 * Compare the complete default ordering, including surface-area and dimension
	 * tie-breaks. Successful results must contain all boxes without overlap.
	 */
	@Test
	void agreesWithIndependentIntegerCoordinateEnumerationOnSmallInputs() {
		List<List<BoxItem>> cases = List.of(
				List.of(item(2, 2, 1), item(2, 1, 1), item(1, 1, 1)),
				List.of(item(3, 1, 1), item(2, 2, 1)),
				List.of(new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 3)),
				List.of(item(2, 2, 1), item(2, 2, 1), item(2, 2, 1)));
		Container container = container(3, 3, 2);
		try(BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
			for(List<BoxItem> items : cases) {
				List<Box> boxes = new ArrayList<>();
				for(BoxItem item : items) {
					for(int i = 0; i < item.getCount(); i++) {
						boxes.add(item.getBox());
					}
				}
				BoundingBox expected = enumerateCoordinates(boxes, container, new ArrayList<>(), null);
				BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
						.withBoxItems(items).withContainer(container).build();
				if(expected == null) {
					assertThat(result.getObjectiveResults()).isEmpty();
				} else {
					assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(expected);
				}
				assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
				BruteForceBoundingBoxResult withoutPruning = boundingBox.newResultBuilder()
						.withBoxItems(items).withContainer(container).withGoal(bounds -> false).build();
				if(expected == null) {
					assertThat(withoutPruning.getObjectiveResults()).isEmpty();
				} else {
					assertThat(withoutPruning.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(expected);
				}
				if(result.isSuccess()) {
					long volume = 0;
					for(Box box : boxes) {
						volume += box.getVolume();
					}
					assertLayout(result, boxes.size(), volume);
				}
			}
		}
	}

	// Deliberately independent of point calculators, iterators, and search pruning.
	private BoundingBox enumerateCoordinates(List<Box> boxes, Container container, List<Placement> placed, BoundingBox best) {
		if(placed.size() == boxes.size()) {
			BoundingBox candidate = bounds(placed);
			return best == null || BoundingBox.MIN_VOLUME.compare(candidate, best) < 0 ? candidate : best;
		}
		for(BoxStackValue value : boxes.get(placed.size()).getStackValues()) {
			for(int x = 0; x <= container.getLoadDx() - value.getDx(); x++) {
				for(int y = 0; y <= container.getLoadDy() - value.getDy(); y++) {
					for(int z = 0; z <= container.getLoadDz() - value.getDz(); z++) {
						Placement candidate = new Placement(value, 0, x, y, z, false);
						boolean intersects = false;
						for(Placement existing : placed) {
							intersects |= candidate.intersects3D(existing);
						}
						if(!intersects) {
							placed.add(candidate);
							best = enumerateCoordinates(boxes, container, placed, best);
							placed.remove(placed.size() - 1);
						}
					}
				}
			}
		}
		return best;
	}

	private static void assertLayout(BruteForceBoundingBoxResult result, int count, long volume) {
		assertThat(result.isSuccess()).isTrue();
		Stack stack = result.getObjectiveResults().get("default").getStack();
		assertThat(stack.size()).isEqualTo(count);
		assertThat(stack.getVolume()).isEqualTo(volume);
		assertThat(stack.getWeight()).isEqualTo(count);
		assertThat(bounds(stack.getPlacements())).isEqualTo(result.getObjectiveResults().get("default").getBoundingBox());
		for(int i = 0; i < count; i++) {
			Placement placement = stack.getPlacements().get(i);
			assertThat(placement.getIndex()).isEqualTo(i);
			for(int j = 0; j < i; j++) {
				assertThat(placement.intersects3D(stack.getPlacements().get(j))).isFalse();
			}
		}
	}

	private static BoundingBox bounds(List<Placement> placements) {
		int dx = 0, dy = 0, dz = 0;
		for(Placement placement : placements) {
			dx = Math.max(dx, placement.getAbsoluteEndX() + 1);
			dy = Math.max(dy, placement.getAbsoluteEndY() + 1);
			dz = Math.max(dz, placement.getAbsoluteEndZ() + 1);
		}
		return new BoundingBox(dx, dy, dz);
	}

	private static BoxItem item(int dx, int dy, int dz) {
		return new BoxItem(Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build());
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}
}
