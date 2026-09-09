package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackagerConstraintTest;

/**
 * PlainPackager integration tests for the {@code maxLoadBoxCount} constraint.
 * <p>
 * {@code maxLoadBoxCount = N} on a box means that at most {@code N} boxes may
 * be stacked anywhere above it (counting the full depth of the column, not just
 * the immediately touching layer).
 */
public class PlainPackagerBoxCountConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// BC-1  count=1 — exactly one box above is accepted
	// -----------------------------------------------------------------------

	/**
	 * Bottom has {@code maxLoadBoxCount=1}.  One box placed on top stays within
	 * the limit — both fit in a single container.
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+
	 *    |    B     |   B — 1 level above A  → count=1 ≤ 1  ✓ accepted
	 *  1 +----------+
	 *    |    A     |   maxLoadBoxCount = 1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B ]
	 * </pre>
	 */
	@Test
	void count1_twoBoxesAccepted() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder().withId("box")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(box, 2)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// BC-2  count=1 — third box in column rejected
	// -----------------------------------------------------------------------

	/**
	 * Three identical boxes with {@code maxLoadBoxCount=1}.  The third box would
	 * be 2 levels above the first, exceeding the limit — it is packed into a
	 * second container.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+   C — 2 levels above A  → count=2 > 1  ✗ rejected
	 *    |    C     |     → goes to container-2
	 *  2 +----------+
	 *    |    B     |   1 level above A → ok
	 *  1 +----------+
	 *    |    A     |   maxLoadBoxCount = 1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B ]
	 *  container-2: [ C ]
	 * </pre>
	 */
	@Test
	void count1_thirdBoxRejected() {
		Container c = container(10, 10, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder().withId("box")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(box, 3)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 2);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// BC-3  count=2 — three boxes in column accepted
	// -----------------------------------------------------------------------

	/**
	 * Bottom has {@code maxLoadBoxCount=2}.  Three boxes can form a single
	 * column without violating the limit.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+
	 *    |    C     |   C — 2 levels above A  → count=2 ≤ 2  ✓ accepted
	 *  2 +----------+
	 *    |    B     |   1 level above A → ok
	 *  1 +----------+
	 *    |    A     |   maxLoadBoxCount = 2
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B, C ]
	 * </pre>
	 */
	@Test
	void count2_threeBoxesAccepted() {
		Container c = container(10, 10, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder().withId("box")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(2).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(box, 3)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 3);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// BC-4  count=2 — fourth box in column rejected
	// -----------------------------------------------------------------------

	/**
	 * Four boxes with {@code maxLoadBoxCount=2}: the fourth would sit 3 levels
	 * above the first, which exceeds the limit of 2.
	 *
	 * <pre>
	 *  z |
	 *  4 +----------+   D — 3 levels above A  → count=3 > 2  ✗ rejected
	 *    |    D     |     → goes to container-2
	 *  3 +----------+
	 *    |    C     |   2 levels above A → ok (at the limit)
	 *  2 +----------+
	 *    |    B     |   1 level above A → ok
	 *  1 +----------+
	 *    |    A     |   maxLoadBoxCount = 2
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B, C ]
	 *  container-2: [ D ]
	 * </pre>
	 */
	@Test
	void count2_fourthBoxRejected() {
		Container c = container(10, 10, 4);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder().withId("box")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(2).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(box, 4)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 3);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// BC-5  Two side-by-side columns — counts are independent per column
	// -----------------------------------------------------------------------

	/**
	 * Two narrow columns in a wide container.  Each column's bottom box has
	 * {@code maxLoadBoxCount=1}: each column independently allows exactly one
	 * box on top.
	 *
	 * <pre>
	 *  z |
	 *  2 +----+----+
	 *    | BL | BR |   B-left, B-right — each 1 level above its base  ✓
	 *  1 +----+----+
	 *    | AL | AR |   both have maxLoadBoxCount = 1
	 *  0 +----+----+
	 *      0  5   10  x
	 *
	 *  container-1: [ AL, AR, BL, BR ]  (all four boxes fit)
	 * </pre>
	 */
	@Test
	void twoColumnsCountsAreIndependent() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			// two base boxes — count=1 each
			items.add(new BoxItem(Box.newBuilder().withId("base")
					.withSize(5, 10, 1).withWeight(1).withMaxLoadBoxCount(1).withRotate2D().build(), 2));
			// two top boxes — no constraint themselves
			items.add(new BoxItem(Box.newBuilder().withId("top")
					.withSize(5, 10, 1).withWeight(1).withRotate2D().build(), 2));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(items)
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 4);
		} finally {
			packager.close();
		}
	}
}
