package com.github.skjolber.packing.packer.plain;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackagerConstraintTest;

/**
 * PlainPackager integration tests for the {@code maxLoadIdenticalBoxCount}
 * (identical-only stacking) constraint.
 * <p>
 * {@code withMaxLoadIdenticalBoxCount(N)} on a box enforces two rules:
 * <ol>
 *   <li>Only boxes of the <em>same type</em> (same {@link Box} object) may
 *       be placed directly on top of it.</li>
 *   <li>The chain of same-type boxes above it may be at most {@code N} deep.</li>
 * </ol>
 */
public class PlainPackagerIdenticalBoxConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// IB-1  Same type, count=1 — two identical boxes accepted
	// -----------------------------------------------------------------------

	/**
	 * Bottom box allows exactly 1 identical box on top.  Placing one identical
	 * box is within the limit.
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+
	 *    |    A2    |   A2 — same type, 1st in chain  → count=1 ≤ 1  ✓
	 *  1 +----------+
	 *    |    A1    |   maxLoadIdenticalBoxCount = 1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A1, A2 ]
	 * </pre>
	 */
	@Test
	void identicalCount1_twoBoxesAccepted() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(a, 2)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// IB-2  Same type, count=1 — third identical box rejected
	// -----------------------------------------------------------------------

	/**
	 * Three identical boxes with {@code maxLoadIdenticalBoxCount=1}.  The third
	 * would be 2 levels above the first, violating the depth limit.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+   A3 — 2nd identical above A1  → count=2 > 1  ✗ rejected
	 *    |    A3    |     → goes to container-2
	 *  2 +----------+
	 *    |    A2    |   1st identical above A1 → ok
	 *  1 +----------+
	 *    |    A1    |   maxLoadIdenticalBoxCount = 1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A1, A2 ]
	 *  container-2: [ A3 ]
	 * </pre>
	 */
	@Test
	void identicalCount1_thirdBoxRejected() {
		Container c = container(10, 10, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(a, 3)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 2);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// IB-3  Different box type cannot be placed on identical-only box
	// -----------------------------------------------------------------------

	/**
	 * Box A requires identical-only stacking.  Box B (different type) cannot
	 * go on top of A — it goes beside A in the same container instead.
	 *
	 * <pre>
	 *  z |
	 *  1 +----+
	 *    | B  |    B cannot go on A → placed beside A at z=0
	 *  0 +----+----+
	 *    | B  | A  |   A: maxLoadIdenticalBoxCount=1  (only identical on top)
	 *    +----+----+
	 *      0  5   10  x
	 *
	 *  container-1 (10×10×2): [ A at x=5, B at x=0 ]  — both at z=0, side by side
	 *  (No box is stacked on A because B is a different type)
	 * </pre>
	 */
	@Test
	void differentTypeCannotStackOnIdenticalOnlyBox() {
		Container c = container(10, 10, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(5, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(1).withRotate2D().build();
			Box b = Box.newBuilder().withId("B")
					.withSize(5, 10, 2).withWeight(1).withRotate2D().build();

			// 1 A + 1 B: B cannot go on A (different type), but fits beside it
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(a, 1), new BoxItem(b, 1)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// IB-4  Same type, count=2 — three identical boxes accepted
	// -----------------------------------------------------------------------

	/**
	 * With {@code maxLoadIdenticalBoxCount=2} three identical boxes form a
	 * valid column: the deepest identical box is 2 levels above the bottom.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+
	 *    |    A3    |   A3 — 2nd identical above A1  → count=2 ≤ 2  ✓ accepted
	 *  2 +----------+
	 *    |    A2    |   1st identical above A1 → ok
	 *  1 +----------+
	 *    |    A1    |   maxLoadIdenticalBoxCount = 2
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A1, A2, A3 ]
	 * </pre>
	 */
	@Test
	void identicalCount2_threeBoxesAccepted() {
		Container c = container(10, 10, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(2).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(a, 3)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 3);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// IB-5  Same type, count=2 — fourth identical box rejected
	// -----------------------------------------------------------------------

	/**
	 * Four identical boxes with {@code maxLoadIdenticalBoxCount=2}.  The fourth
	 * would be 3 levels above the first (count=3 > 2) and is rejected.
	 *
	 * <pre>
	 *  z |
	 *  4 +----------+   A4 — 3rd identical above A1  → count=3 > 2  ✗ rejected
	 *    |    A4    |     → goes to container-2
	 *  3 +----------+
	 *    |    A3    |   2nd identical above A1 → ok (at the limit)
	 *  2 +----------+
	 *    |    A2    |   1st identical above A1 → ok
	 *  1 +----------+
	 *    |    A1    |   maxLoadIdenticalBoxCount = 2
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A1, A2, A3 ]
	 *  container-2: [ A4 ]
	 * </pre>
	 */
	@Test
	void identicalCount2_fourthBoxRejected() {
		Container c = container(10, 10, 4);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(2).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(a, 4)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 3);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}
}
