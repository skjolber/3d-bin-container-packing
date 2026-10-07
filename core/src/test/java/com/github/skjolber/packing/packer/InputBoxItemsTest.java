package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Packing does not change the box items passed in, so that they can be packed again.
 */
public class InputBoxItemsTest {

	//
	//  containers 1 x 1 x 1: three boxes, three containers
	//
	//  [a]   [a]   [a]
	//
	@Test
	public void packingDoesNotChangeTheBoxItemCounts() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());

		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			AbstractPackager<?> packager = supplier.get();
			try {
				BoxItem boxItem = new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 3);
				List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(1, 1, 1).withMaxLoadWeight(100).build(), 3));
				String name = packager.getClass().getSimpleName();
				for (int i = 0; i < 2; i++) {
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers)
							.withBoxItems(List.of(boxItem))
							.withMaxContainerCount(3)
							.build();
					assertThat(result.isSuccess()).as(name + " packing " + i).isTrue();
					assertThat(result.getContainers()).as(name + " packing " + i).hasSize(3);
					assertThat(boxItem.getCount()).as(name + " packing " + i).isEqualTo(3);
				}
			} finally {
				packager.close();
			}
		}
	}
}
