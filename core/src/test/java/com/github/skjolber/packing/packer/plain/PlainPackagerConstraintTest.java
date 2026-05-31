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
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

/**
 * Integration tests for {@link PlainPackager} verifying that per-box
 * {@code maxLoadWeight} and {@code maxLoadBoxCount} constraints on
 * {@link com.github.skjolber.packing.api.BoxStackValue} are honoured
 * during packing.
 *
 * <p>The packager automatically enables {@link com.github.skjolber.packing.packer.LoadAwarePlacementControls}
 * whenever at least one box in the item list declares a load constraint.
 */
public class PlainPackagerConstraintTest extends AbstractPackagerTest {

	// -----------------------------------------------------------------------
	// maxLoadWeight – basic stacking acceptance / rejection
	// -----------------------------------------------------------------------

	/**
	 * A box whose {@code maxLoadWeight} is smaller than the weight of the box
	 * being placed on top prevents stacking: the two boxes must go into
	 * separate containers.
	 *
	 * <pre>
	 *  Attempt (rejected):        Result (2 containers):
	 *
	 *  z                          Container 1   Container 2
	 *  |
	 *  2  +----------+
	 *     | B w=10   |  weight > maxLoadWeight    [A]           [B]
	 *  1  ----------- ← rejected                z=0           z=0
	 *     | A w=10   |  maxLoadWeight=5
	 *  0  +----------+
	 *     0         10  x
	 * </pre>
	 */
	@Test
	void testMaxLoadWeight_stackingRejected() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			// Box A: sits on floor; maxLoadWeight=5 means at most 5 units may rest on it
			items.add(new BoxItem(Box.newBuilder()
					.withId("A")
					.withSize(10, 10, 1)
					.withWeight(10)
					.withMaxLoadWeight(5)
					.withRotate2D()
					.build(), 1));
			// Box B: weight 10, far too heavy for A's maxLoadWeight=5
			items.add(new BoxItem(Box.newBuilder()
					.withId("B")
					.withSize(10, 10, 1)
					.withWeight(10)
					.withRotate2D()
					.build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 5))
					.withMaxContainerCount(5)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			// B (weight=10) cannot stack on A (maxLoadWeight=5) → 2 separate containers
			assertThat(result.getContainers()).hasSize(2);
			for (Container c : result.getContainers()) {
				assertThat(c.getStack().size()).isEqualTo(1);
			}
		} finally {
			packager.close();
		}
	}

	/**
	 * When the stacked box's weight is within the supporter's
	 * {@code maxLoadWeight}, both boxes fit in a single container on two levels.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     | B w=3    |  weight(3) ≤ maxLoadWeight(5) → accepted
	 *  1  +----------+
	 *     | A w=3    |  maxLoadWeight=5
	 *  0  +----------+
	 *     0         10  x
	 * </pre>
	 */
	@Test
	void testMaxLoadWeight_stackingAllowed() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder()
					.withId("A")
					.withSize(10, 10, 1)
					.withWeight(3)
					.withMaxLoadWeight(5)
					.withRotate2D()
					.build(), 1));
			items.add(new BoxItem(Box.newBuilder()
					.withId("B")
					.withSize(10, 10, 1)
					.withWeight(3)
					.withRotate2D()
					.build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(2);
		} finally {
			packager.close();
		}
	}

	/**
	 * Verifies that {@code loadWeight} values are correctly recorded on
	 * placements after a successful two-level pack.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     | top w=7  |  loadWeight expected = 0
	 *  1  +----------+
	 *     | bot w=5  |  loadWeight expected = 7  (top's weight rests on it)
	 *  0  +----------+
	 *     0         10  x
	 * </pre>
	 */
	@Test
	void testMaxLoadWeight_loadWeightRecordedOnPlacements() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			// bottom box: heavy enough to be sorted first (larger weight = picked first by comparator)
			items.add(new BoxItem(Box.newBuilder()
					.withId("bot")
					.withSize(10, 10, 1)
					.withWeight(5)
					.withMaxLoadWeight(20)
					.withRotate2D()
					.build(), 1));
			items.add(new BoxItem(Box.newBuilder()
					.withId("top")
					.withSize(10, 10, 1)
					.withWeight(7)
					.withRotate2D()
					.build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);

			List<Placement> placements = result.getContainers().get(0).getStack().getPlacements();
			assertThat(placements).hasSize(2);

			// find the bottom and top placements by z coordinate
			Placement bottom = placements.stream()
					.filter(p -> p.getAbsoluteZ() == 0)
					.findFirst().orElseThrow();
			Placement top = placements.stream()
					.filter(p -> p.getAbsoluteZ() == 1)
					.findFirst().orElseThrow();

			// bottom bears the weight of top
			assertThat(bottom.getLoadWeight()).isEqualTo(top.getWeight());
			// top has nothing above it
			assertThat(top.getLoadWeight()).isEqualTo(0);
		} finally {
			packager.close();
		}
	}

	/**
	 * Verifies that {@code loadWeight} propagates through three levels.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     | C  w=4   |  loadWeight = 0
	 *  2  +----------+
	 *     | B  w=3   |  loadWeight = 4
	 *  1  +----------+
	 *     | A  w=2   |  loadWeight = 7  (B.weight + C.weight)
	 *  0  +----------+
	 *     0         10  x
	 * </pre>
	 */
	@Test
	void testMaxLoadWeight_loadWeightPropagatesThreeLevels() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 3)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(2).withMaxLoadWeight(50).withRotate2D().build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("B")
					.withSize(10, 10, 1).withWeight(3).withMaxLoadWeight(50).withRotate2D().build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("C")
					.withSize(10, 10, 1).withWeight(4).withRotate2D().build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);

			List<Placement> placements = result.getContainers().get(0).getStack().getPlacements();
			assertThat(placements).hasSize(3);

			Placement p0 = placementAt(placements, 0);
			Placement p1 = placementAt(placements, 1);
			Placement p2 = placementAt(placements, 2);

			// p2 is topmost – nothing above it
			assertThat(p2.getLoadWeight()).isEqualTo(0);
			// p1 bears p2's weight
			assertThat(p1.getLoadWeight()).isEqualTo(p2.getWeight());
			// p0 bears p1.weight + p2.weight
			assertThat(p0.getLoadWeight()).isEqualTo(p1.getWeight() + p2.getWeight());
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// maxLoadBoxCount – stacking depth enforcement
	// -----------------------------------------------------------------------

	/**
	 * {@code maxLoadBoxCount = 1} means exactly one box may be stacked on top.
	 * With three boxes in a single column the third box (two levels above the
	 * bottom) is rejected.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +--------+
	 *     |    C   |  ← rejected: A has maxLoadBoxCount=1, levels=2 > 1
	 *  2  +--------+
	 *     |    B   |  1 level above A → ok (1 ≤ 1)
	 *  1  +--------+
	 *     |    A   |  maxLoadBoxCount=1  (exactly 1 box on top allowed)
	 *  0  +--------+
	 *     0       10  x
	 *
	 *  Result: A+B in container-1, C in container-2.
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCount1_thirdLevelRejected() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 3)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(1)
					.withMaxLoadBoxCount(1)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 3));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 5))
					.withMaxContainerCount(5)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			// only 1 box on top allowed → 2 boxes per container
			assertThat(result.getContainers()).hasSize(2);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(2);
			assertThat(result.getContainers().get(1).getStack().size()).isEqualTo(1);
		} finally {
			packager.close();
		}
	}

	/**
	 * {@code maxLoadBoxCount = 2} permits two boxes stacked on top.
	 * Three boxes (one bottom + two levels above) fit in one container.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  2 levels above A, levels(2) ≤ maxLoadBoxCount(2) → accepted
	 *  2  +----------+
	 *     |    B     |  1 level above A → accepted
	 *  1  +----------+
	 *     |    A     |  maxLoadBoxCount=2  (2 boxes on top allowed)
	 *  0  +----------+
	 *     0         10  x
	 *
	 *  Result: A+B+C in one container.
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCount2_thirdLevelAllowed() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 3)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(1)
					.withMaxLoadBoxCount(2)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 3));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(3);
		} finally {
			packager.close();
		}
	}

	/**
	 * {@code maxLoadBoxCount = 2} permits two boxes on top.  Four boxes in a
	 * single column: A+B+C fit (C is 2 levels above A, within count=2), but D
	 * would be 3 levels above A which exceeds count=2.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  4  +--------+
	 *     |    D   |  ← rejected: 3 levels above A, 3 > maxLoadBoxCount(2)
	 *  3  +--------+
	 *     |    C   |  2 levels above A → ok (2 ≤ 2)
	 *  2  +--------+
	 *     |    B   |  1 level above A → ok
	 *  1  +--------+
	 *     |    A   |  maxLoadBoxCount=2  (2 boxes on top allowed)
	 *  0  +--------+
	 *
	 *  Result: A+B+C in container-1, D in container-2.
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCount2_fourthLevelRejected() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 4)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(1)
					.withMaxLoadBoxCount(2)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 4));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 5))
					.withMaxContainerCount(5)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			// maxLoadBoxCount=2: 2 boxes on top allowed → 3 boxes per column
			assertThat(result.getContainers()).hasSize(2);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(3);
			assertThat(result.getContainers().get(1).getStack().size()).isEqualTo(1);
		} finally {
			packager.close();
		}
	}

	/**
	 * {@code maxLoadBoxCount = 1} permits exactly one box on top of each bottom
	 * box.  Two side-by-side columns in a 20×10×2 container: each column has
	 * its bottom box with one box on top (1 ≤ count=1) → all 4 fit.
	 *
	 * <pre>
	 *  Top view (z=0):
	 *
	 *   y
	 *   |
	 *  10  +----------+----------+
	 *      |  col-L   |  col-R   |
	 *   0  +----------+----------+
	 *      0         10         20  x
	 *
	 *  Side view (one column):
	 *
	 *  z
	 *  |
	 *  2  +----------+
	 *     |  top     |  1 box above bottom, 1 ≤ maxLoadBoxCount(1) → accepted
	 *  1  +----------+
	 *     |  bottom  |  maxLoadBoxCount=1  (1 box on top allowed)
	 *  0  +----------+
	 *
	 *  Result: all 4 boxes in 1 container (2 side-by-side × 1 level each).
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCount1_twoBySideAllowedOneLevel() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(20, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(1)
					.withMaxLoadBoxCount(1)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 4));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(4);
		} finally {
			packager.close();
		}
	}

	/**
	 * {@code maxLoadBoxCount = 2} (2 boxes on top) with two side-by-side
	 * columns: 4 boxes still fit in one container because each column has
	 * only 1 box above its bottom box (1 ≤ count=2).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+----------+
	 *     |  col-L 2 |  col-R 2 |  1 box above each bottom, 1 ≤ count(2) → ok
	 *  1  +----------+----------+
	 *     |  col-L 1 |  col-R 1 |  maxLoadBoxCount=2 (2 boxes on top allowed)
	 *  0  +----------+----------+
	 *     0         10         20  x
	 *
	 *  Result: all 4 boxes in 1 container.
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCount2_twoBySideAllowed() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(20, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(1)
					.withMaxLoadBoxCount(2)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 4));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(1);
			assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(4);
		} finally {
			packager.close();
		}
	}

	/**
	 * Combining maxLoadWeight and maxLoadBoxCount: both constraints must
	 * pass simultaneously. Here the box count allows three boxes on top but the
	 * weight of the second box exceeds the first box's maxLoadWeight, so
	 * only one box fits per container.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +--------+
	 *     | B w=10 |  weight(10) > maxLoadWeight(5) → rejected
	 *  1  x--------x  (not placed here)
	 *     | A w=10 |  maxLoadBoxCount=3 (3 boxes on top allowed), maxLoadWeight=5
	 *  0  +--------+
	 *
	 *  Result: 2 containers (A in one, B in another).
	 * </pre>
	 */
	@Test
	void testMaxLoadBoxCountAndWeight_bothEnforced() {
		Container container = Container.newBuilder()
				.withDescription("c")
				.withEmptyWeight(0)
				.withSize(10, 10, 2)
				.withMaxLoadWeight(1000)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			// Box type A: count allows stacking (maxLoadBoxCount=3), but weight is too high for maxLoadWeight=5
			Box box = Box.newBuilder()
					.withId("box")
					.withSize(10, 10, 1)
					.withWeight(10)
					.withMaxLoadBoxCount(3)
					.withMaxLoadWeight(5)
					.withRotate2D()
					.build();

			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(box, 2));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 5))
					.withMaxContainerCount(5)
					.withBoxItems(items)
					.build();

			assertThat(result.isSuccess()).isTrue();
			// Weight check prevents stacking even though box count allows it
			assertThat(result.getContainers()).hasSize(2);
			for (Container c : result.getContainers()) {
				assertThat(c.getStack().size()).isEqualTo(1);
			}
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static Placement placementAt(List<Placement> placements, int z) {
		return placements.stream()
				.filter(p -> p.getAbsoluteZ() == z)
				.findFirst()
				.orElseThrow(() -> new AssertionError("No placement at z=" + z));
	}
}
