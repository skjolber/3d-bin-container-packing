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
 * Visualization tests for the {@code maxLoadWeight} box constraint.
 * <p>
 * Each test packs the same scenario used in the JMH weight-constraint benchmark
 * and writes the result to {@code containers.json} for inspection in the viewer.
 * <p>
 * Scenario: 8 heavy base boxes (w=10, maxLoadWeight=5) + 8 heavy top boxes
 * (w=10).  Every top box exceeds the base's load limit and overflows to a new
 * container.
 *
 * <pre>
 *   z
 *   2 +----------+  top (w=10): 10 &gt; maxLoadWeight=5  ✗ → next container
 *     |  top w=10|
 *   1 +----------+
 *     |  bot w=10|  maxLoadWeight = 5
 *   0 +----------+
 *       0       10  x
 *
 *   ×8 pairs → 16 containers total (1 box each)
 * </pre>
 */
public class WeightConstraintVisualizationTest extends AbstractPackagerTest {

	private static final List<BoxItem> ITEMS = List.of(
			new BoxItem(Box.newBuilder().withId("wBot")
					.withSize(10, 10, 1).withWeight(10).withMaxLoadWeight(5).build(), 8),
			new BoxItem(Box.newBuilder().withId("wTop")
					.withSize(10, 10, 1).withWeight(10).build(), 8));

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("weight-container")
						.withEmptyWeight(0)
						.withSize(10, 10, 2)
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
