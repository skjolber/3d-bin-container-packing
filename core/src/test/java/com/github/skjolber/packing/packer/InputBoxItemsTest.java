package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Packing does not change the box items passed in, so that they can be packed again, and the placements of results
 * refer to them.
 */
public class InputBoxItemsTest {

	//
	//  containers 1 x 1 x 1: three boxes, three containers
	//
	//  [a]   [a]   [a]
	//
	private static final List<Supplier<AbstractPackager<?>>> PACKAGERS = List.of(
			() -> PlainPackager.newBuilder().build(),
			() -> LargestAreaFitFirstPackager.newBuilder().build(),
			() -> FastLargestAreaFitFirstPackager.newBuilder().build(),
			() -> BruteForcePackager.newBuilder().build(),
			() -> FastBruteForcePackager.newBuilder().build(),
			() -> ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());

	@Test
	public void packingDoesNotChangeTheBoxItemCounts() {
		for (Supplier<AbstractPackager<?>> supplier : PACKAGERS) {
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

	//
	//  container 3 x 1 x 1, packed twice:
	//
	//  [a][a][b]
	//
	@Test
	public void placementsReferToTheBoxItemsPassedIn() {
		for (Supplier<AbstractPackager<?>> supplier : PACKAGERS) {
			AbstractPackager<?> packager = supplier.get();
			try {
				BoxItem a = new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 2);
				BoxItem b = new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 1);
				List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(3, 1, 1).withMaxLoadWeight(100).build(), 1));
				String name = packager.getClass().getSimpleName();
				for (int i = 0; i < 2; i++) {
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers)
							.withBoxItems(List.of(a, b))
							.withMaxContainerCount(1)
							.build();
					assertThat(result.isSuccess()).as(name + " packing " + i).isTrue();
					List<Placement> placements = result.get(0).getStack().getPlacements();
					assertThat(placements).extracting(Placement::getBoxItem).as(name + " packing " + i)
							.containsExactlyInAnyOrder(a, a, b)
							.allSatisfy(boxItem -> assertThat(boxItem).isIn(a, b));
				}
			} finally {
				packager.close();
			}
		}
	}

	//
	//  container 3 x 1 x 1, groups x and y:
	//
	//  [x][x][y]
	//
	@Test
	public void placementsReferToTheGroupsPassedIn() {
		for (Supplier<AbstractPackager<?>> supplier : PACKAGERS) {
			AbstractPackager<?> packager = supplier.get();
			try {
				BoxItem a = new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 2);
				BoxItem b = new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 1);
				BoxItemGroup x = new BoxItemGroup("x", List.of(a));
				BoxItemGroup y = new BoxItemGroup("y", List.of(b));
				List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(3, 1, 1).withMaxLoadWeight(100).build(), 1));
				String name = packager.getClass().getSimpleName();
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers)
						.withBoxItemGroups(List.of(x, y))
						.withMaxContainerCount(1)
						.build();
				assertThat(result.isSuccess()).as(name).isTrue();
				List<Placement> placements = result.get(0).getStack().getPlacements();
				assertThat(placements).extracting(Placement::getBoxItem).as(name).containsExactlyInAnyOrder(a, a, b);
				assertThat(placements).extracting(p -> p.getBoxItem().getGroup()).as(name).containsExactlyInAnyOrder(x, x, y);
				assertThat(x.getItems()).containsExactly(a);
				assertThat(a.getCount()).isEqualTo(2);
			} finally {
				packager.close();
			}
		}
	}
}
