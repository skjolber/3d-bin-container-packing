package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Container types in preference order, all of which hold the box; the first is used:
 *
 * <pre>
 *   c0 [b][ ]      c1 [b][ ][ ]      c2 ...
 * </pre>
 *
 * With three or more container types, the packager searches for it, and reuses a result in other containers which
 * also hold it.
 */
public class FirstFittingContainerTest {

	@Test
	public void usesTheFirstContainerWhichHoldsTheBoxes() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build());

		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			AbstractPackager<?> packager = supplier.get();
			try {
				for(int count : new int[] {2, 4}) {
					// one container, or several (then the packagers first check whether a single container holds the boxes)
					for(int maxContainerCount : new int[] {1, 2}) {
						List<ContainerItem> containers = new ArrayList<>();
						for(int i = 0; i < count; i++) {
							containers.add(new ContainerItem(Container.newBuilder().withId("c" + i).withSize(2 + i, 1, 1).withMaxLoadWeight(100).build(), 1));
						}
						PackagerResult result = packager.newResultBuilder()
								.withContainerItems(containers)
								.withBoxItems(List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1)))
								.withMaxContainerCount(maxContainerCount)
								.build();

						assertThat(result.getContainers())
								.as("%s, %d container types, max %d containers", packager.getClass().getSimpleName(), count, maxContainerCount)
								.extracting(Container::getId)
								.containsExactly("c0");
					}
				}
			} finally {
				packager.close();
			}
		}
	}
}
