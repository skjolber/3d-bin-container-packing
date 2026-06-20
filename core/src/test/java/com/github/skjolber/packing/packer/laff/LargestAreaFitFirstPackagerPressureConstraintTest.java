package com.github.skjolber.packing.packer.laff;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackagerConstraintTest;

/**
 * LargestAreaFitFirstPackager integration tests for the
 * {@code maxLoadPressure} constraint.
 */
public class LargestAreaFitFirstPackagerPressureConstraintTest extends AbstractPackagerConstraintTest {

	// -----------------------------------------------------------------------
	// P-1  Small base — heavy top exceeds pressure limit
	// -----------------------------------------------------------------------

	/**
	 * Bottom box has a small base (2×5 = area 10) and low pressure limit.
	 * A heavy top box (w=11) produces pressure above the limit and is rejected.
	 * <p>
	 * Both boxes weigh 11 so the comparator ties; insertion order places
	 * "bot" on the floor first.  "top" (w=11) then exceeds bot's pressure limit
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
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
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
		} finally {
			packager.close();
		}
	}

	// -----------------------------------------------------------------------
	// P-2  Exact pressure boundary — accepted
	// -----------------------------------------------------------------------

	/**
	 * Top box weight equals exactly the pressure limit × area — accepted.
	 *
	 * <pre>
	 *  z |
	 *  2 +----+
	 *    |top |   top (w=10)  weight 10 = 1×10 = maxPressure×area  ✓ accepted
	 *  1 +----+
	 *    |bot |   2×5 base (area=10), maxLoadPressure=1  → maxWeight = 10
	 *  0 +----+
	 *      0 2  x
	 *
	 *  container-1: [ bot, top ]
	 * </pre>
	 */
	@Test
	void exactPressureBoundaryAccepted() {
		Container c = container(2, 5, 2);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
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
	 *    |    top    |   top (w=11)  11 &lt; 1×55=55  ✓ accepted
	 *  1 +-----------+
	 *    |    bot    |   11×5 base (area=55), maxLoadPressure=1  → maxWeight=55
	 *  0 +-----------+
	 *      0        11  x
	 *
	 *  container-1: [ bot, top ]
	 * </pre>
	 */
	@Test
	void sameWeightOnLargerBaseAccepted() {
		Container c = container(11, 5, 2);
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
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
	 * The bottom box has a pressure limit.  A light middle box fits; placing a
	 * heavy box C on top of B would propagate 11 units to A, which exceeds
	 * A's pressure limit (maxWeight = 1×10 = 10).
	 * <p>
	 * A is heaviest (w=20) so it is placed on the floor.  C (w=11) is rejected
	 * at z=1 directly on A (11 &gt; 10), so B (w=1) lands at z=1.  C then tries
	 * z=2 on B, but 11 still propagates to A → rejected.
	 *
	 * <pre>
	 *  z |
	 *  3 +----+   C (w=11)  11 &gt; maxLoadPressure×area = 1×10 = 10  ✗ rejected
	 *    |  C |             (propagates to A whether on B or directly on A)
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
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
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
			assertStackSize(result, 0, 2);
			assertStackSize(result, 1, 1);
		} finally {
			packager.close();
		}
	}
}
