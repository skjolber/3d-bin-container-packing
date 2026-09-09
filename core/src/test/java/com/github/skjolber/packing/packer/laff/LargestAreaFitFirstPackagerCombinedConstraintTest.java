package com.github.skjolber.packing.packer.laff;

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
 * LargestAreaFitFirstPackager integration tests verifying that multiple box
 * constraints work correctly when active simultaneously.
 */
public class LargestAreaFitFirstPackagerCombinedConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// C-1  Weight binds before box-count — rejected by weight
	// -----------------------------------------------------------------------

	/**
	 * The box allows up to 3 boxes stacked on top (boxCount=3), but the top
	 * box's weight exceeds maxLoadWeight=5.  Weight is the binding constraint.
	 * <p>
	 * Both boxes weigh 10 so the comparator ties; insertion order places "bot"
	 * on the floor first.  "top" (w=10) then exceeds bot's maxLoadWeight=5 and
	 * goes to container-2.
	 *
	 * <pre>
	 *  z |
	 *  2 +----------+   top (w=10) → 10 &gt; maxLoadWeight 5  ✗ rejected by weight
	 *    |  top w=10|   (boxCount=3 would allow it)
	 *  1 +----------+
	 *    |  bot w=10|   maxLoadWeight=5  AND  maxLoadBoxCount=3
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ bot ]
	 *  container-2: [ top ]
	 * </pre>
	 */
	@Test
	void weightBindsBeforeBoxCount() {
		Container c = container(10, 10, 2);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Box bot = Box.newBuilder().withId("bot")
					.withSize(10, 10, 1).withWeight(10)
					.withMaxLoadWeight(5).withMaxLoadBoxCount(3)
					.build();
			Box top = Box.newBuilder().withId("top")
					.withSize(10, 10, 1).withWeight(10).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(bot, 1), new BoxItem(top, 1)))
					.build();

			assertContainers(result, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// C-2  Box-count binds before weight — rejected by count
	// -----------------------------------------------------------------------

	/**
	 * Light boxes (weight well within maxLoadWeight=100), but the column depth
	 * of 3 exceeds maxLoadBoxCount=1.  Count is the binding constraint.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+   C — count=2 > 1  ✗ rejected by count
	 *    |    C w=1 |   (weight 2 ≤ 100 would be fine)
	 *  2 +----------+
	 *    |    B w=1 |   count=1 ≤ 1  ✓
	 *  1 +----------+
	 *    |    A w=1 |   maxLoadWeight=100  AND  maxLoadBoxCount=1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B ]
	 *  container-2: [ C ]
	 * </pre>
	 */
	@Test
	void boxCountBindsBeforeWeight() {
		Container c = container(10, 10, 3);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Box box = Box.newBuilder().withId("box")
					.withSize(10, 10, 1).withWeight(1)
					.withMaxLoadWeight(100).withMaxLoadBoxCount(1)
					.build();

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
	// C-3  Pressure binds when weight would allow
	// -----------------------------------------------------------------------

	/**
	 * Both maxLoadWeight and maxLoadPressure are active.  The top box satisfies
	 * maxLoadWeight but any non-zero weight violates maxLoadPressure=0.
	 *
	 * <pre>
	 *  z |
	 *  2 +----+
	 *    |top |   top (w=1)  weight 1 ≤ 10 ✓, but 1 > 0×10=0  ✗ pressure fails
	 *  1 +----+
	 *    |bot |   2×5 base (area=10), maxLoadWeight=10, maxLoadPressure=0
	 *  0 +----+
	 *      0 2  x
	 *
	 *  container-1: [ bot ]
	 *  container-2: [ top ]
	 * </pre>
	 */
	@Test
	void pressureBindsWhenWeightWouldAllow() {
		Container c = container(2, 5, 2);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Box bot = Box.newBuilder().withId("bot")
					.withSize(2, 5, 1).withWeight(1).withMaxLoadWeight(10).withMaxLoadPressure(0)
					.withRotate2D().build();
			Box top = Box.newBuilder().withId("top")
					.withSize(2, 5, 1).withWeight(1).withRotate2D().build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(bot, 1), new BoxItem(top, 1)))
					.build();

			assertContainers(result, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// C-4  Identical-only + box count — fourth box rejected by depth
	// -----------------------------------------------------------------------

	/**
	 * Box type A has {@code maxLoadIdenticalBoxCount=2}.  Three A boxes form a
	 * valid column (depth=2 at the limit).  The fourth is rejected.
	 *
	 * <pre>
	 *  z |
	 *  4 +----------+
	 *    |    A4    |   A4 — depth=3 > 2  ✗ rejected → container-2
	 *  3 +----------+
	 *    |    A3    |   depth=2 ≤ 2  ✓ ok (at limit)
	 *  2 +----------+
	 *    |    A2    |   depth=1 ≤ 2  ✓ ok
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
	void identicalAndBoxCount_fourthRejected() {
		Container c = container(10, 10, 4);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(2)
					.build();

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

	// -----------------------------------------------------------------------
	// C-5  Weight + pressure + box count — three constraints, count binds
	// -----------------------------------------------------------------------

	/**
	 * Three constraints active simultaneously.  Weight and pressure are
	 * generous; only box count creates the actual rejection.
	 *
	 * <pre>
	 *  z |
	 *  3 +----------+
	 *    |    C w=1 |   C — weight ok, pressure ok, count=2>1  ✗ rejected
	 *  2 +----------+
	 *    |    B w=1 |   count=1 ≤ 1  ✓
	 *  1 +----------+
	 *    |    A w=1 |   maxLoadWeight=100, maxLoadPressure=100, maxLoadBoxCount=1
	 *  0 +----------+
	 *      0       10  x
	 *
	 *  container-1: [ A, B ]
	 *  container-2: [ C ]
	 * </pre>
	 */
	@Test
	void tripleConstraint_boxCountBinds() {
		Container c = container(10, 10, 3);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("A")
					.withSize(10, 10, 1).withWeight(1)
					.withMaxLoadWeight(100).withMaxLoadPressure(100).withMaxLoadBoxCount(1)
					.build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("B")
					.withSize(10, 10, 1).withWeight(1).build(), 1));
			items.add(new BoxItem(Box.newBuilder().withId("C")
					.withSize(10, 10, 1).withWeight(1).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(items)
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 2);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}
}
