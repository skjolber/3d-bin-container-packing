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
 * Visualization tests for the {@code maxLoadBoxCount} box constraint.
 * <p>
 * Each test packs the same scenario used in the JMH box-count-constraint
 * benchmark and writes the result to {@code containers.json} for inspection
 * in the viewer.
 * <p>
 * Scenario: 24 identical boxes (maxLoadBoxCount=2) in 10×10×3 containers.
 * Each container holds one column of 3 boxes (depth=2 ≤ 2); a 4th box would
 * be rejected.  With a single-column container, boxes fill groups of 3 and
 * overflow, exercising the box-count check on every placement.
 *
 * <pre>
 *   z
 *   3 +----------+  4th box: depth=3 &gt; maxLoadBoxCount=2  ✗ → next container
 *   2 +----------+
 *     |   box    |  depth=2  ✓ (at limit)
 *   1 +----------+
 *     |   box    |  depth=1  ✓
 *   0 +----------+
 *     |   box    |  maxLoadBoxCount = 2
 *       0       10  x
 *
 *   24 boxes → 8 containers of 3 boxes each
 * </pre>
 */
public class BoxCountConstraintVisualizationTest extends AbstractPackagerTest {

	private static final List<BoxItem> ITEMS = List.of(
			new BoxItem(Box.newBuilder().withId("bcBox")
					.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(2).build(), 24));

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("boxcount-container")
						.withEmptyWeight(0)
						.withSize(10, 10, 3)
						.withMaxLoadWeight(1_000_000)
						.build(), 12)
				.build();
	}

	@Test
	void plainPackager() throws Exception {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers())
					.withMaxContainerCount(12)
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
					.withMaxContainerCount(12)
					.withBoxItems(ITEMS)
					.build();
			write(result);
		} finally {
			packager.close();
		}
	}
}
