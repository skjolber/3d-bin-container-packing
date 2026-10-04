package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * Box item groups accepted one container at a time, so that the remaining groups are accepted in later containers.
 */
public class BruteForceBoxItemGroupsTest {

	@Test
	public void bruteForcePacksAGroupPerContainer() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertPacksAGroupPerContainer(packager);
		}
	}

	@Test
	public void fastBruteForcePacksAGroupPerContainer() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertPacksAGroupPerContainer(packager);
		}
	}

	@Test
	public void parallelBruteForcePacksAGroupPerContainer() {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertPacksAGroupPerContainer(packager);
		}
	}

	@Test
	public void bruteForcePacksAGroupWhichFitsOneContainerType() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		}
	}

	@Test
	public void fastBruteForcePacksAGroupWhichFitsOneContainerType() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		}
	}

	@Test
	public void parallelBruteForcePacksAGroupWhichFitsOneContainerType() {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		}
	}

	/**
	 * One of the groups is too large for the small container type, first or second. Groups are packed in order,
	 * so when it comes first, the small container is used for the second group after the big container:
	 *
	 * <pre>
	 *   big: [long  ]      small: [c]
	 * </pre>
	 */
	private static void assertPacksAGroupWhichFitsOneContainerType(AbstractPackager<?> packager) {
		assertPacksAGroupWhichFitsOneContainerType(packager, false);
		assertPacksAGroupWhichFitsOneContainerType(packager, true);
	}

	private static void assertPacksAGroupWhichFitsOneContainerType(AbstractPackager<?> packager, boolean longFirst) {
		BoxItemGroup cube = new BoxItemGroup("cube", List.of(new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 1)));
		BoxItemGroup longGroup = new BoxItemGroup("long", List.of(new BoxItem(Box.newBuilder().withId("long").withSize(2, 1, 1).withWeight(1).build(), 1)));
		List<BoxItemGroup> groups = longFirst ? List.of(longGroup, cube) : List.of(cube, longGroup);
		List<ContainerItem> containers = List.of(
				new ContainerItem(Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(10).build(), 1),
				new ContainerItem(Container.newBuilder().withId("big").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 1));

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItemGroups(groups)
				.withMaxContainerCount(2)
				.withInterruptDuration(10_000)
				.build();

		assertThat(result.getContainers())
				.extracting(c -> c.getId() + ":" + c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
				.as("long first: %s", longFirst)
				.containsExactlyInAnyOrder("big:long", "small:cube");
	}

	/**
	 * Four groups of one unit cube, each container holds one:
	 *
	 * <pre>
	 *   [a]   [b]   [c]   [d]
	 * </pre>
	 */
	private static void assertPacksAGroupPerContainer(AbstractPackager<?> packager) {
		List<BoxItemGroup> groups = new ArrayList<>();
		for(String id : List.of("a", "b", "c", "d")) {
			groups.add(new BoxItemGroup(id, List.of(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1))));
		}
		Container container = Container.newBuilder().withId("cube").withSize(1, 1, 1).withMaxLoadWeight(1).build();

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 4)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(4)
				.withInterruptDuration(10_000)
				.build();

		assertThat(result.getContainers())
				.extracting(c -> c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
				.containsExactlyInAnyOrder("a", "b", "c", "d");
	}
}
