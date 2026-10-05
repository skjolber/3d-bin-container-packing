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
public class BruteForceBoxItemGroupAcceptanceTest {

	@Test
	public void bruteForcePacksAGroupPerContainer() {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			assertPacksAGroupPerContainer(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForcePacksAGroupPerContainer() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			assertPacksAGroupPerContainer(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void parallelBruteForcePacksAGroupPerContainer() {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			assertPacksAGroupPerContainer(packager);
		} finally {
			packager.close();
		}
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
		Container container = Container.newBuilder().withId("cube").withSize(1, 1, 1).withMaxLoadWeight(100).build();

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 4)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(4)
				.build();

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers())
				.extracting(c -> c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
				.containsExactlyInAnyOrder("a", "b", "c", "d");
	}
}
