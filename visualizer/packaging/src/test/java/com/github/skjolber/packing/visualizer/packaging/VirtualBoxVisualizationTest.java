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
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.virtualbox.VirtualBoxPackager;

/**
 * Writes an order of many identical boxes packed directly and with virtual-box preprocessing (identical boxes grouped
 * into filled layouts before packing), to compare them in the viewer (key R).
 */
public class VirtualBoxVisualizationTest extends AbstractPackagerTest {

	@Test
	void directAndWithVirtualBoxes() throws Exception {
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("container").withSize(12, 10, 6).withMaxLoadWeight(10_000).build(), 1)
				.build();
		List<BoxItem> boxItems = List.of(
				new BoxItem(Box.newBuilder().withId("A").withSize(3, 2, 2).withRotate3D().withWeight(2).build(), 40),
				new BoxItem(Box.newBuilder().withId("B").withSize(4, 4, 2).withRotate3D().withWeight(3).build(), 6));

		Map<String, PackagerResult> results = new LinkedHashMap<>();
		try (PlainPackager delegate = PlainPackager.newBuilder().build();
				VirtualBoxPackager packager = new VirtualBoxPackager(delegate)) {
			results.put("plain", delegate.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(1).build());
			results.put("plain, virtual boxes", packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(1).withInterruptDuration(5_000).build());
		}
		try (DefaultValidator validator = new DefaultValidator()) {
			write(results, validator.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(boxItems)
					.withMaxContainerCount(1));
		}
	}
}
