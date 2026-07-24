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
 * PlainPackager integration tests for the {@code maxLoadPressure} constraint.
 * <p>
 * Pressure is defined as {@code weight × 1000 / area}.  A box with
 * {@code maxLoadPressure = P} rejects a placement if
 * {@code topBoxWeight > P × (dx × dy)} (where dx×dy is the overlap area).
 * <p>
 * Practical rule: a box with base area {@code A} and {@code maxLoadPressure=P}
 * can bear at most {@code P × A} units of weight on top.
 */
public class PlainPackagerPressureConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// P-1  Small base — heavy top exceeds pressure limit
	// -----------------------------------------------------------------------

	/**
	 * Bottom box has a small base (2×5 = area 10) and low pressure limit.
	 * A heavy top box (w=11) produces too high a pressure and is rejected.
	 * <p>
	 * Both boxes weigh 11 so the comparator ties and insertion order places
	 * "bot" on the floor first. "top" (w=11) then exceeds bot's pressure limit
	 * (maxLoadPressure=1 → maxWeight = 1×10 = 10) and goes to container-2.
	 *
	 * <pre>
	 *  z |
	 *  2 +----+
	 *    |top |   top (w=11)  11 &gt; maxLoadPressure×area = 1×10 = 10  ✗ rejected
	 *  1 +----+
	 *    |bot |   2×5 base (area=10), maxLoadPressure=1  → maxWeight = 10
	 *  0 +----+
	 *      0 2  x  (y spans 0..4)
	 *
	 *  container-1: [ bot ]
	 *  container-2: [ top ]
	 * </pre>
	 */
	@Test
	void heavyBoxExceedsPressureLimit() {
		Container c = container(2, 5, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box bot = Box.newBuilder().withId("bot")
					.withSize(2, 5, 1).withWeight(11).withMaxLoadPressure(1).withRotate2D().build();
			Box top = Box.newBuilder().withId("top")
					.withSize(2, 5, 1).withWeight(11).withRotate2D().build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(bot, 1), new BoxItem(top, 1)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 1);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// P-2  Exact pressure boundary — accepted
	// -----------------------------------------------------------------------

	/**
	 * Top box weight equals exactly the pressure limit × area — accepted
	 * (boundary is inclusive: constraint fails only when strictly greater).
	 *
	 * <pre>
	 *  z |
	 *  2 +----+
	 *    |top |   top (w=10)  pressure = 10/10 = 1.0  = maxPressure=1  ✓ accepted
	 *  1 +----+
	 *    |bot |   2×5 base (area=10), maxLoadPressure = 1  → maxWeight = 10
	 *  0 +----+
	 *      0 2  x
	 *
	 *  container-1: [ bot, top ]
	 * </pre>
	 */
	@Test
	void exactPressureBoundaryAccepted() {
		Container c = container(2, 5, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box bot = Box.newBuilder().withId("bot")
					.withSize(2, 5, 1).withWeight(1).withMaxLoadPressure(1).withRotate2D().build();
			Box top = Box.newBuilder().withId("top")
					.withSize(2, 5, 1).withWeight(10).withRotate2D().build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(bot, 1), new BoxItem(top, 1)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// P-3  Larger base absorbs the same weight under the pressure limit
	// -----------------------------------------------------------------------

	/**
	 * Same weight (w=11), but the bottom box has a larger base (11×5 = area 55).
	 * The resulting pressure (11/55 ≈ 0.2) is well within the limit of 1.
	 *
	 * <pre>
	 *  z |
	 *  2 +-----------+
	 *    |    top    |   top (w=11)  pressure ≈ 0.2  < maxPressure=1  ✓ accepted
	 *  1 +-----------+
	 *    |    bot    |   11×5 base (area=55), maxLoadPressure = 1  → maxWeight = 55
	 *  0 +-----------+
	 *      0        11  x
	 *
	 *  container-1: [ bot, top ]
	 * </pre>
	 */
	@Test
	void sameWeightOnLargerBaseAccepted() {
		Container c = container(11, 5, 2);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box bot = Box.newBuilder().withId("bot")
					.withSize(11, 5, 1).withWeight(1).withMaxLoadPressure(1).withRotate2D().build();
			Box top = Box.newBuilder().withId("top")
					.withSize(11, 5, 1).withWeight(11).withRotate2D().build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 1))
					.withMaxContainerCount(1)
					.withBoxItems(List.of(new BoxItem(bot, 1), new BoxItem(top, 1)))
					.build();

			assertContainers(result, 1);
			assertStackSize(result, 0, 2);
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// P-4  Pressure propagates through supporter chain
	// -----------------------------------------------------------------------

	/**
	 * The bottom box has a pressure limit.  A light middle box (B, w=1) fits;
	 * a heavy box C (w=11) placed on top of B would propagate 11 units to A,
	 * which exceeds A's pressure limit (maxWeight = 1×10 = 10).
	 * <p>
	 * A is heaviest (w=20) so it is placed on the floor first.  B (w=1) is
	 * lighter than C (w=11), but C is rejected at z=1 on A (11 &gt; 10), so B
	 * lands at z=1.  C then tries z=2 on B, but 11 still propagates to A → rejected.
	 *
	 * <pre>
	 *  z |
	 *  3 +----+   C (w=11)  11 &gt; maxLoadPressure×area = 1×10 = 10  ✗ rejected
	 *    |  C |             (propagates to A whether placed on B or directly on A)
	 *  2 +----+
	 *    |  B |   w=1; no constraint
	 *  1 +----+
	 *    |  A |   2×5 base (area=10), maxLoadPressure=1  → maxWeight=10
	 *  0 +----+
	 *      0 2  x
	 *
	 *  container-1: [ A, B ]
	 *  container-2: [ C ]
	 * </pre>
	 */
	@Test
	void pressurePropagatesDownChain() {
		Container c = container(2, 5, 3);
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Box a = Box.newBuilder().withId("A")
					.withSize(2, 5, 1).withWeight(20).withMaxLoadPressure(1).withRotate2D().build();
			Box b = Box.newBuilder().withId("B")
					.withSize(2, 5, 1).withWeight(1).withRotate2D().build();
			Box cc = Box.newBuilder().withId("C")
					.withSize(2, 5, 1).withWeight(11).withRotate2D().build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(c, 2))
					.withMaxContainerCount(2)
					.withBoxItems(List.of(new BoxItem(a, 1), new BoxItem(b, 1), new BoxItem(cc, 1)))
					.build();

			assertContainers(result, 2);
			assertStackSize(result, 0, 2);  // A + B
			assertStackSize(result, 1, 1);  // C alone
		} finally {
			packager.close();
		}
	}
}
