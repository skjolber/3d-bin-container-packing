package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Box item groups are packed whole: a session rejects a result from another packager which holds only part of a
 * group, and is unchanged afterwards.
 *
 * <pre>
 *   group a: [a0][a1]     result: [a0]
 * </pre>
 */
public class PartialGroupAcceptanceTest {

	private static Supplier<AbstractPackager<?>> packager(String name) {
		switch (name) {
			case "plain": return () -> PlainPackager.newBuilder().build();
			case "laff": return () -> LargestAreaFitFirstPackager.newBuilder().build();
			case "fastLaff": return () -> FastLargestAreaFitFirstPackager.newBuilder().build();
			case "bruteForce": return () -> BruteForcePackager.newBuilder().build();
			case "fastBruteForce": return () -> FastBruteForcePackager.newBuilder().build();
			default: throw new IllegalArgumentException(name);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff", "bruteForce", "fastBruteForce" })
	public void rejectsPartOfAGroup(String name) throws Exception {
		try (AbstractPackager<?> packager = packager(name).get()) {
			List<BoxItem> boxItems = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId("a" + i).withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			List<BoxItemGroup> groups = List.of(new BoxItemGroup("a", boxItems));
			Container container = Container.newBuilder().withId("c").withSize(2, 1, 1).withMaxLoadWeight(100).build();
			PackagerSession session = packager.createSession(new PackagerInput(null, groups, List.of(new ContainerItem(container, 2)), 2, Order.NONE), () -> false);

			Stack stack = new Stack();
			stack.add(new Placement(boxItems.get(0), boxItems.get(0).getBox().getStackValue(0), 0, 0, 0, 0));
			assertThatThrownBy(() -> session.accept(new DefaultIntermediatePackagerResult(session.getContainerItem(0), stack)))
					.isInstanceOf(IllegalArgumentException.class);

			assertThat(session.countRemainingBoxes()).isEqualTo(2);
			assertThat(session.getContainerItem(0).getCount()).isEqualTo(2);
		}
	}
}
