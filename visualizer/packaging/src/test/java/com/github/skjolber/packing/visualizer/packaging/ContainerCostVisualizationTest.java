package com.github.skjolber.packing.visualizer.packaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * Writes the same order packed without and with container costs, to compare them in the viewer (key R).
 *
 * <pre>
 *  small: 10 x 10 x 10, cost 100      large: 20 x 10 x 10, cost 250
 *
 *  three boxes of 5 x 10 x 10 fit in two small containers (cost 200) or one large (cost 250)
 * </pre>
 *
 * Without costs, the packager prefers one container which holds all the boxes (the large); with costs, the cheapest
 * combination wins (two small).
 */
public class ContainerCostVisualizationTest extends AbstractPackagerTest {

	@Test
	void withAndWithoutCosts() throws Exception {
		Container small = Container.newBuilder().withId("small").withSize(10, 10, 10).withMaxLoadWeight(1000).build();
		Container large = Container.newBuilder().withId("large").withSize(20, 10, 10).withMaxLoadWeight(1000).build();
		List<BoxItem> boxItems = List.of(new BoxItem(Box.newBuilder().withId("half").withSize(5, 10, 10).withWeight(10).build(), 3));

		List<ContainerItem> withoutCosts = ContainerItem.newListBuilder()
				.withContainer(small, 2)
				.withContainer(large, 1)
				.build();
		List<ContainerItem> withCosts = ContainerItem.newListBuilder()
				.withContainer(small, 2, new FixedContainerCostCalculator(100, small.getMaxLoadVolume(), "small", 0))
				.withContainer(large, 1, new FixedContainerCostCalculator(250, large.getMaxLoadVolume(), "large", 0))
				.build();

		Map<String, PackagerResult> results = new LinkedHashMap<>();
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			results.put("without costs", packager.newResultBuilder().withContainerItems(withoutCosts).withBoxItems(boxItems).withMaxContainerCount(2).build());
			results.put("with costs", packager.newResultBuilder().withContainerItems(withCosts).withBoxItems(boxItems).withMaxContainerCount(2).build());
		}
		try (DefaultValidator validator = new DefaultValidator()) {
			write(results, validator.newResultBuilder()
					.withContainerItems(withoutCosts)
					.withBoxItems(boxItems)
					.withMaxContainerCount(2));
		}
	}
}
