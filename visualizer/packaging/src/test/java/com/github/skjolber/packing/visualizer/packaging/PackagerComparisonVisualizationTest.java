package com.github.skjolber.packing.visualizer.packaging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * Writes the results of several packagers for the same order, to compare them in the viewer (key R, or click a row
 * in the comparison table): the containers used, the volume, the time, and whether the results are valid.
 */
public class PackagerComparisonVisualizationTest extends AbstractPackagerTest {

	private static final long INTERRUPT_DURATION = 5_000;

	@Test
	void compareOnOneOrder() throws Exception {
		// containers from small to large, the order of preference; with this order, plain and LAFF use the large
		// container, while fast brute force and the composite fit the boxes into the medium one
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("small").withSize(10, 10, 10).withMaxLoadWeight(1000).build(), 3)
				.withContainer(Container.newBuilder().withId("medium").withSize(15, 10, 10).withMaxLoadWeight(1000).build(), 3)
				.withContainer(Container.newBuilder().withId("large").withSize(20, 15, 10).withMaxLoadWeight(1000).build(), 3)
				.build();

		Random random = new Random(2);
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Box box = Box.newBuilder()
					.withId("box-" + i)
					.withSize(3 + random.nextInt(6), 3 + random.nextInt(6), 2 + random.nextInt(5))
					.withRotate3D()
					.withWeight(1 + random.nextInt(10))
					.build();
			boxItems.add(new BoxItem(box, 1 + random.nextInt(2)));
		}

		Map<String, Supplier<AbstractPackager<?>>> packagers = new LinkedHashMap<>();
		packagers.put("plain", () -> PlainPackager.newBuilder().build());
		packagers.put("laff", () -> LargestAreaFitFirstPackager.newBuilder().build());
		packagers.put("fast brute force", () -> FastBruteForcePackager.newBuilder().build());
		packagers.put("composite: plain, fast brute force", () -> CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build(), 1000)
				.build());

		Map<String, PackagerResult> results = new LinkedHashMap<>();
		for (Map.Entry<String, Supplier<AbstractPackager<?>>> entry : packagers.entrySet()) {
			try (AbstractPackager<?> packager = entry.getValue().get()) {
				results.put(entry.getKey(), packager.newResultBuilder()
						.withContainerItems(containers)
						.withBoxItems(boxItems)
						.withMaxContainerCount(3)
						.withInterruptDuration(INTERRUPT_DURATION)
						.build());
			}
		}

		try (DefaultValidator validator = new DefaultValidator()) {
			write(results, validator.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(boxItems)
					.withMaxContainerCount(3));
		}
	}
}
