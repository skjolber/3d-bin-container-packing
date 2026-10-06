package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * An interrupted packaging attempt stops, also while placing the boxes of box item groups.
 */
public class PlainPackagerGroupInterruptTest {

	@Test
	public void interruptedAttemptWithGroupsStops() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItemGroup> groups = new ArrayList<>();
			for (String id : new String[] {"a", "b"}) {
				List<BoxItem> boxItems = new ArrayList<>();
				for (int i = 0; i < 3; i++) {
					boxItems.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(1, 1, 1).withWeight(1).build(), 1));
				}
				groups.add(new BoxItemGroup(id, boxItems));
			}
			Container container = Container.newBuilder().withDescription("c").withSize(3, 2, 1).withMaxLoadWeight(100).build();
			List<ControlledContainerItem> containers = new ArrayList<>();
			containers.add(new ControlledContainerItem(container, 1));

			PackagerAdapter adapter = packager.new PlainBoxItemGroupAdapter(groups, Order.NONE, new ContainerItemsCalculator(containers), () -> true);

			assertThatThrownBy(() -> adapter.attempt(0, null, false)).isInstanceOf(PackagerInterruptedException.class);
		} finally {
			packager.close();
		}
	}
}
