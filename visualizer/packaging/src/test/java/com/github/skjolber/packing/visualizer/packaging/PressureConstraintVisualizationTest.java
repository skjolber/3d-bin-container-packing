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
 * Visualization tests for the {@code maxLoadPressure} box constraint.
 * <p>
 * Each test packs the same scenario used in the JMH pressure-constraint benchmark
 * and writes the result to {@code containers.json} for inspection in the viewer.
 * <p>
 * Scenario: 8 narrow base boxes (2×5 base, area=10, maxLoadPressure=1) +
 * 8 heavy top boxes (w=11).  Since 11 &gt; 1×10 = 10, every top box exceeds the
 * pressure limit and overflows to a new container.
 *
 * <pre>
 *   z
 *   2 +----+  top (w=11): 11 &gt; pressure×area = 1×10 = 10  ✗ → next container
 *     |top |
 *   1 +----+
 *     |bot |  2×5 base (area=10), maxLoadPressure=1  → maxWeight = 10
 *   0 +----+
 *       0 2  x   (y spans 0..4)
 *
 *   ×8 pairs → 16 containers total (1 box each)
 * </pre>
 */
public class PressureConstraintVisualizationTest extends AbstractPackagerTest {

	private static final List<BoxItem> ITEMS = List.of(
			new BoxItem(Box.newBuilder().withId("pBot")
					.withSize(2, 5, 1).withWeight(11).withMaxLoadPressure(1).withRotate2D().build(), 8),
			new BoxItem(Box.newBuilder().withId("pTop")
					.withSize(2, 5, 1).withWeight(11).withRotate2D().build(), 8));

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("pressure-container")
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
