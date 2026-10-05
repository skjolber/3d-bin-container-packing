package com.github.skjolber.packing.visualizer.packaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultsVisualizer;

/**
 * Deliveries: the extraction order (the boxes of a stop can be taken out without moving boxes for later stops) and
 * the container priority (urgent boxes in the first containers).
 */
public class DeliveryVisualizationTest extends AbstractPackagerTest {

	//
	//  side view (z up), door at x = 30
	//
	//  12 +------------------------------+
	//     |  stop 3  |  stop 2  | stop 1 |   door ->
	//   0 +------------------------------+
	//     0                             30  x
	//
	/**
	 * A van with a door, and three stops: the same order packed without and with extraction orders (key R; press C for
	 * the extraction colour mode). Without them, boxes for the first stop are blocked by boxes for later stops
	 * (outlined in red); with them, the boxes for each stop are between the door and the boxes for later stops.
	 */
	@Test
	void stops() throws Exception {
		Container van = Container.newBuilder().withId("van").withSize(30, 15, 12).withMaxLoadWeight(1000).withAccess(ContainerAccess.FRONT).build();
		List<ContainerItem> containers = ContainerItem.newListBuilder().withContainer(van, 1).build();

		Map<String, Integer> stops = new HashMap<>();
		Map<String, PackagerResult> results = new LinkedHashMap<>();
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			PackagerResult withoutStops = packager.newResultBuilder().withContainerItems(containers).withBoxItems(deliveries(stops, false)).withMaxContainerCount(1).build();
			// the same stops, for the validation
			for (Container container : withoutStops.getContainers()) {
				for (Placement placement : container.getStack().getPlacements()) {
					placement.getBoxItem().withExtractionOrder(stops.get(placement.getBoxItem().getBox().getId()));
				}
			}
			results.put("without stops", withoutStops);
			results.put("three stops", packager.newResultBuilder().withContainerItems(containers).withBoxItems(deliveries(stops, true)).withMaxContainerCount(1).build());
		}
		PackagingResultsVisualizer visualization = new DefaultPackagingResultVisualizerFactory(true).visualize(results, null);
		assertThat(results.values()).allMatch(PackagerResult::isSuccess);
		assertThat(visualization.getResults().get(0).isValid()).isFalse();
		assertThat(visualization.getResults().get(1).isValid()).isTrue();
		Files.writeString(OUTPUT.toPath(), visualization.toJson(), StandardCharsets.UTF_8);
	}

	private static List<BoxItem> deliveries(Map<String, Integer> stops, boolean extractionOrder) {
		Random random = new Random(5);
		List<BoxItem> items = new ArrayList<>();
		for (int stop = 1; stop <= 3; stop++) {
			for (int i = 0; i < 3; i++) {
				String id = "stop" + stop + "-" + i;
				BoxItem item = new BoxItem(Box.newBuilder().withId(id).withSize(3 + random.nextInt(5), 3 + random.nextInt(5), 2 + random.nextInt(4)).withRotate3D().withWeight(1).build(), 2);
				if(extractionOrder) {
					item.withExtractionOrder(stop);
				}
				stops.put(id, stop);
				items.add(item);
			}
		}
		return items;
	}

	//
	//  pallets 10 x 10 x 8; the number is the container priority
	//
	//    first       second       third
	//  +-------+   +-------+   +-------+
	//  | 0 0 0 |   | 0 1 1 |   | 1 2 2 |   urgent boxes (0) first, then 1, then 2
	//  +-------+   +-------+   +-------+
	//
	/**
	 * Boxes in containers in order of their container priority: urgent boxes in the first containers (hover a box for
	 * its priority).
	 */
	@Test
	void containerPriorities() throws Exception {
		Container pallet = Container.newBuilder().withId("pallet").withSize(10, 10, 8).withMaxLoadWeight(1000).build();
		Random random = new Random(7);
		List<BoxItem> items = new ArrayList<>();
		String[] names = {"urgent", "normal", "later"};
		for (int priority = 0; priority < names.length; priority++) {
			for (int i = 0; i < 3; i++) {
				items.add(new BoxItem(Box.newBuilder().withId(names[priority] + "-" + i).withSize(3 + random.nextInt(4), 3 + random.nextInt(4), 2 + random.nextInt(4)).withRotate3D().withWeight(1).build(), 2)
						.withContainerPriority(priority));
			}
		}
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(ContainerItem.newListBuilder().withContainer(pallet, 6).build())
					.withBoxItems(items)
					.withMaxContainerCount(6)
					.build();
			assertThat(result.isSuccess()).isTrue();
			assertThat(result.size()).isGreaterThan(1);
			PackagingResultsVisualizer visualization = new PackagingResultsVisualizer(new DefaultPackagingResultVisualizerFactory(true).visualize(result));
			assertThat(visualization.getResults().get(0).isValid()).isTrue();
			Files.writeString(OUTPUT.toPath(), visualization.toJson(), StandardCharsets.UTF_8);
		}
	}
}
