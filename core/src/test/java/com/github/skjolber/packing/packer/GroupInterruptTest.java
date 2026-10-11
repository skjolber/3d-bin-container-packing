package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * An interrupted packaging attempt stops, also while placing the boxes of box item groups.
 */
public class GroupInterruptTest {

	private static Supplier<AbstractPackager<?>> packager(String name) {
		switch (name) {
			case "plain": return () -> PlainPackager.newBuilder().build();
			case "laff": return () -> LargestAreaFitFirstPackager.newBuilder().build();
			case "fastLaff": return () -> FastLargestAreaFitFirstPackager.newBuilder().build();
			default: throw new IllegalArgumentException(name);
		}
	}

	private static List<BoxItem> boxItems(String prefix) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId(prefix + i).withSize(1, 1, 1).withWeight(1).build(), 1));
		}
		return boxItems;
	}

	private static List<ContainerItem> containers() {
		return List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(3, 2, 1).withMaxLoadWeight(100).build(), 1));
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void interruptedAttemptWithBoxItemsStops(String name) throws Exception {
		try (AbstractPackager<?> packager = packager(name).get()) {
			PackagerSession session = packager.createSession(new PackagerInput(boxItems("a"), null, containers(), 1, Order.NONE), () -> true);
			assertThatThrownBy(() -> session.attempt(0, null, false)).isInstanceOf(PackagerInterruptedException.class);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void interruptedAttemptWithGroupsStops(String name) throws Exception {
		try (AbstractPackager<?> packager = packager(name).get()) {
			List<BoxItemGroup> groups = List.of(new BoxItemGroup("a", boxItems("a")), new BoxItemGroup("b", boxItems("b")));
			PackagerSession session = packager.createSession(new PackagerInput(null, groups, containers(), 1, Order.NONE), () -> true);
			assertThatThrownBy(() -> session.attempt(0, null, false)).isInstanceOf(PackagerInterruptedException.class);
		}
	}
}
