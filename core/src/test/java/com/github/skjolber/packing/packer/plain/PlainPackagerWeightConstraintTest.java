package com.github.skjolber.packing.packer.plain;

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
import com.github.skjolber.packing.packer.AbstractPackagerConstraintTest;

/**
 * PlainPackager integration tests for the {@code maxLoadWeight} box constraint.
 * <p>
 * Each box can declare the maximum weight that may rest on top of it.  When
 * the accumulated weight of boxes placed above exceeds that limit the packager
 * must reject the placement and start a new container (or leave the box out).
 */
public class PlainPackagerWeightConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// W-1  Overweight box rejected to new container
	// -----------------------------------------------------------------------

	/**
	 * A single heavy box placed on a weight-constrained base exceeds the limit
	 * and must be packed into a second container.
	 * <p>
	 * Both boxes weigh 10 so the comparator produces a tie; insertion order
	 * places "bot" on the floor first.  "top" (w=10) then exceeds bot's
	 * maxLoadWeight=5 and is rejected into container-2.
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+   ← top (w=10) REJECTED on bot (maxLoadWeight=5)
	 *    |  top w=10|     goes to container-2
	 *  1 +----------+
	 *    |  bot w=10|   maxLoadWeight = 5
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ bot ]
	 *  container-2: [ top ]
	 * </pre>
	 */
	@Test
	void overweightBoxRejectedToNewContainer() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("bot")
					.withSize(10, 10, 1).withWeight(10).withMaxLoadWeight(5).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("top")
					.withSize(10, 10, 1).withWeight(10).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(items)
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 1);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// W-2  Exact weight boundary — placement accepted
	// -----------------------------------------------------------------------

	/**
	 * A box whose weight equals exactly the bottom's {@code maxLoadWeight}
	 * is accepted (boundary is inclusive).
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+
	 *    |  top w=5 |   ← top (w=5) ACCEPTED — exactly at limit
	 *  1 +----------+
	 *    |  bot w=1 |   maxLoadWeight = 5
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ bot, top ]  (single container)
	 * </pre>
	 */
	@Test
	void exactWeightBoundaryAccepted() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("bot")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(5).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("top")
					.withSize(10, 10, 1).withWeight(5).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// W-3  Chain-weight propagation — constraint on bottom visible 2 levels up
	// -----------------------------------------------------------------------

	/**
	 * The bottom box has a weight limit.  A middle box is placed within that
	 * limit.  A third box whose weight, added to the middle's, would exceed the
	 * bottom's limit is rejected.
	 * <p>
	 * A is heaviest (w=10) so the comparator places it on the floor first.
	 * B and C are equal-weight (w=3) so insertion order ensures B lands at z=1.
	 * C then tries z=2 but A would bear 3+3=6 &gt; 5, so C is rejected.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+
	 *    |   C w=3  |   ← C (w=3) REJECTED — A would bear 3+3=6 &gt; 5
	 *  2 +----------+
	 *    |   B w=3  |   no constraint; already adds 3 to A
	 *  1 +----------+
	 *    |   A w=10 |   maxLoadWeight = 5
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B ]
	 *  container-2: [ C ]
	 * </pre>
	 */
	@Test
	void chainWeightPropagatesDown() {
		Container c = container(10, 10, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(10).withMaxLoadWeight(5).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("B")
					.withSize(10, 10, 1).withWeight(3).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("C")
					.withSize(10, 10, 1).withWeight(3).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(items)
					.build();

			assertContainers(result, 2);
			// container-1 must have A+B; C rejected
			assertStackSize(result, 0, 2);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// W-4  Two side-by-side columns track weight independently
	// -----------------------------------------------------------------------

	/**
	 * When boxes fill two independent columns the weight limit on one column's
	 * base box does not affect the other column.
	 * <p>
	 * "left" and "right" are both heavy (w=10) so the comparator places them at
	 * z=0 before the heavy-top boxes (w=7).  left has maxLoadWeight=5; when one
	 * heavy-top box is tried on left, 7 &gt; 5 is rejected and the packager places
	 * it on right instead.  The second heavy-top box has no valid z=1 slot left
	 * in container-1, so it moves to container-2.
	 *
	 * <pre>
	 *  z |
	 *  2 +----+----+   ← heavy (w=7) OK on right (no limit)
	 *    |    |hvy |     second heavy REJECTED → container-2
	 *  1 +----+----+
	 *    |lft |rgt |   left: w=10 maxLoadWeight=5   right: w=10 no limit
	 *  0 +----+----+
	 *      0  5   10  x
	 *
	 *  container-1: [ left, right, heavy-on-right ]
	 *  container-2: [ heavy-2 ]
	 * </pre>
	 */
	@Test
	void twoColumnWeightsAreIndependent() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("left")
					.withSize(5, 10, 1).withWeight(10).withMaxLoadWeight(5).withRotate2D().build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("right")
					.withSize(5, 10, 1).withWeight(10).withRotate2D().build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("heavy")
					.withSize(5, 10, 1).withWeight(7).withRotate2D().build(), 2));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(items)
					.build();

			// One heavy box fits on the unconstrained right column; the other goes to container-2
			assertContainers(result, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// W-5  loadWeight recorded on placement after packing
	// -----------------------------------------------------------------------

	/**
	 * After a successful two-level pack the bottom placement's {@code loadWeight}
	 * must equal the top box's weight.
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+
	 *    |  top w=7 |   top (w=7)   loadWeight = 0
	 *  1 +----------+
	 *    |  bot w=3 |   maxLoadWeight=20  loadWeight = 7  ← asserted
	 *  0 +----------+
	 *      0       10  x
	 * </pre>
	 */
	@Test
	void loadWeightRecordedOnBottomPlacement() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("bot")
					.withSize(10, 10, 1).withWeight(3).withMaxLoadWeight(20).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("top")
					.withSize(10, 10, 1).withWeight(7).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertContainers(result, 1);
			List<Placement> placements = result.getContainers().get(0).getStack().getPlacements();
			Placement bot = placementAt(placements, 0);
			Placement top = placementAt(placements, 1);
			assertThat(bot.getLoadWeight()).isEqualTo(top.getWeight());
			assertThat(top.getLoadWeight()).isEqualTo(0L);
		} finally {
			packager.close();
		}
	}
}
