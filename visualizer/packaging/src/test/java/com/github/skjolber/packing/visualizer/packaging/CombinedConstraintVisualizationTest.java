package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Visualization tests with all four box constraints active simultaneously:
 * {@code maxLoadWeight}, {@code maxLoadPressure}, {@code maxLoadBoxCount}, and
 * {@code maxLoadIdenticalBoxCount}.
 * <p>
 * Each test packs the same scenario used in the JMH combined-constraint
 * benchmark and writes the result to {@code containers.json} for inspection
 * in the viewer.
 * <p>
 * Scenario: 8 base boxes with all four constraints + 8 unconstrained top boxes.
 * The weight constraint binds first (10 &gt; maxLoadWeight=5); pressure also fails
 * (10/10 = 1.0 &gt; 0.5); box count would allow (1 ≤ 3).  All four checks fire on
 * every placement candidate.
 *
 * <pre>
 *   z
 *   2 +----+  top (w=10):
 *     |top |    weight:   10 &gt; maxLoadWeight=5           ✗ (binds — checked first)
 *   1 +----+    pressure: 10/10 = 1.0 &gt; maxPressure=0.5  ✗ (also fails)
 *     |bot |    count:    1 ≤ maxBoxCount=3               ✓ (would allow)
 *   0 +----+  2×5 base; maxLoadWeight=5, maxLoadPressure=0.5, maxLoadBoxCount=3
 *       0 2  x
 *
 *   ×8 pairs — weight binds, each pair splits into two containers
 * </pre>
 */
public class CombinedConstraintVisualizationTest extends AbstractPackagerTest {

	private static final List<BoxItem> ITEMS = List.of(
			new BoxItem(Box.newBuilder().withId("cBot")
					.withSize(2, 5, 1).withWeight(1)
					.withMaxLoadWeight(5).withMaxLoadPressure(0.5).withMaxLoadBoxCount(3)
					.withRotate2D().build(), 8),
			new BoxItem(Box.newBuilder().withId("cTop")
					.withSize(2, 5, 1).withWeight(10).withRotate2D().build(), 8));

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("combined-container")
						.withEmptyWeight(0)
						.withSize(2, 5, 2)
						.withMaxLoadWeight(1_000_000)
						.build(), 16)
				.build();
	}

	@Test
	void plainPackager() throws Exception {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers())
					.withMaxContainerCount(16)
					.withBoxItems(ITEMS)
					.build();
			write(result);
		} finally {
			packager.close();
		}
	}

	@Test
	void laffPackager() throws Exception {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers())
					.withMaxContainerCount(16)
					.withBoxItems(ITEMS)
					.build();
			write(result);
		} finally {
			packager.close();
		}
	}
}
