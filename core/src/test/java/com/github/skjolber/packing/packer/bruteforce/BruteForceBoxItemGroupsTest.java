package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Brute-force packing of box item groups which do not fit some container types.
 */
public class BruteForceBoxItemGroupsTest {

	@Test
	public void bruteForcePacksAGroupWhichFitsOneContainerType() {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForcePacksAGroupWhichFitsOneContainerType() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void parallelBruteForcePacksAGroupWhichFitsOneContainerType() {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			assertPacksAGroupWhichFitsOneContainerType(packager);
		} finally {
			packager.close();
		}
	}

	/**
	 * One of the groups is too large for the small container type, first or second: it is packed into a big
	 * container, and the other group into any container.
	 *
	 * <pre>
	 *   big: [long  ]      small or big: [c]
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
				new ContainerItem(Container.newBuilder().withId("big").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 2));

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItemGroups(groups)
				.withMaxContainerCount(3)
				.build();

		assertThat(result.isSuccess()).as("long first: %s", longFirst).isTrue();
		assertThat(result.getContainers())
				.extracting(c -> c.getId() + ":" + c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
				.as("long first: %s", longFirst)
				.contains("big:long")
				.hasSize(2);
	}
}
