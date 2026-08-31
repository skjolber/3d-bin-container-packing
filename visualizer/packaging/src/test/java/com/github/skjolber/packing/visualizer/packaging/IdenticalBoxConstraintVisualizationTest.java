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
 * Visualization tests for the {@code maxLoadIdenticalBoxCount} box constraint.
 * <p>
 * Each test packs the same scenario used in the JMH identical-box-constraint
 * benchmark and writes the result to {@code containers.json} for inspection
 * in the viewer.
 * <p>
 * Scenario: two box types in 10×10×3 containers.  Type A (12 items,
 * maxLoadIdenticalBoxCount=2) is capped at 2 deep; type B (12 items, no
 * constraint) fills half-width columns beside type A.  Type B cannot stack
 * on type A (different type), so each fills its own column independently.
 *
 * <pre>
 *   z
 *   3 +----+----+  A3: depth=3 &gt; 2  ✗ rejected   B3: depth=3, no limit ✓
 *     | A3 | B3 |
 *   2 +----+----+
 *     | A2 | B2 |  A2: depth=2 ✓ (at limit)   B2: depth=2, no limit ✓
 *   1 +----+----+
 *     | A1 | B1 |  A1: maxLoadIdenticalBoxCount=2   B1: no constraint
 *   0 +----+----+
 *       0  5   10  x
 *
 *   type-A columns capped at depth 2; type-B columns fill to container height
 * </pre>
 */
public class IdenticalBoxConstraintVisualizationTest extends AbstractPackagerTest {

	private static final List<BoxItem> ITEMS = List.of(
			new BoxItem(Box.newBuilder().withId("ibA")
					.withSize(5, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(2).withRotate2D().build(), 12),
			new BoxItem(Box.newBuilder().withId("ibB")
					.withSize(5, 10, 1).withWeight(1).withRotate2D().build(), 12));

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("identical-container")
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
