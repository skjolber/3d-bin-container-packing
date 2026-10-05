package com.github.skjolber.packing.visualizer.packaging;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Writes a result with box item groups and support calculation, for the viewer's group and support colour
 * modes (key C).
 */
public class SupportAndGroupsVisualizationTest extends AbstractPackagerTest {

	@Test
	void groupsWithSupport() throws Exception {
		List<BoxItemGroup> groups = new ArrayList<>();
		for (int g = 0; g < 3; g++) {
			List<BoxItem> items = new ArrayList<>();
			items.add(new BoxItem(Box.newBuilder().withId("g" + g + "-large").withSize(4, 3, 2).withRotate3D().withWeight(5).build(), 2));
			items.add(new BoxItem(Box.newBuilder().withId("g" + g + "-small").withSize(2, 2, 1).withRotate3D().withWeight(1).build(), 3));
			groups.add(new BoxItemGroup("group-" + g, items));
		}
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("support").withSize(8, 6, 6).withMaxLoadWeight(1000).build(), 2)
				.build();

		try (PlainPackager packager = PlainPackager.newBuilder().withCalculateSupport(true).build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItemGroups(groups)
					.withMaxContainerCount(2)
					.build();
			write(result);
		}
	}
}
