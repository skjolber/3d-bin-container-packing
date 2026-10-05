package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * The placement search covers all boxes of a group, also when the group does not start at the first box.
 */
public class GroupPlacementSearchTest {

	//
	//  top view, container 3 x 2 x 1
	//
	//  2 +-------+-------+---+
	//    |       |       | a |   group b is larger, so it is packed first,
	//  1 |   b   |   b   +---+   but its boxes come after the boxes of group a
	//    |       |       | a |
	//  0 +-------+-------+---+
	//    0       1       2   3  x
	//
	@Test
	void groupsShareAContainer() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build());

		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			AbstractPackager<?> packager = supplier.get();
			try {
				List<BoxItemGroup> groups = new ArrayList<>();
				for (String id : List.of("a", "b")) {
					List<BoxItem> items = new ArrayList<>();
					for (int i = 0; i < 2; i++) {
						int dy = id.equals("a") ? 1 : 2;
						items.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(1, dy, 1).withRotate3D().withWeight(1).build(), 1));
					}
					groups.add(new BoxItemGroup(id, items));
				}
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(ContainerItem.newListBuilder()
								.withContainer(Container.newBuilder().withId("c").withSize(3, 2, 1).withMaxLoadWeight(100).build(), 2)
								.build())
						.withBoxItemGroups(groups)
						.withMaxContainerCount(2)
						.build();

				assertThat(result.isSuccess()).as(packager.getClass().getSimpleName()).isTrue();
				assertThat(result.getContainers()).as(packager.getClass().getSimpleName()).hasSize(1);
				assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(4);
			} finally {
				packager.close();
			}
		}
	}
}
