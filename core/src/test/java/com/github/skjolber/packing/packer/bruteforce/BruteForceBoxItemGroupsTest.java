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
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInput;

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

	@Test
	public void bruteForceAcceptsGroupsOutOfOrder() throws Exception {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertAcceptsGroupsOutOfOrder(packager);
		}
	}

	@Test
	public void fastBruteForceAcceptsGroupsOutOfOrder() throws Exception {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertAcceptsGroupsOutOfOrder(packager);
		}
	}

	@Test
	public void parallelBruteForceAcceptsGroupsOutOfOrder() throws Exception {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertAcceptsGroupsOutOfOrder(packager);
		}
	}

	/**
	 * A result from another packager (for example plain, which packs groups in any order) may hold any of the
	 * remaining groups, not only the first. The session continues with the other groups:
	 *
	 * <pre>
	 *   accepted: [b]      then: [a]   [c]
	 * </pre>
	 */
	private static void assertAcceptsGroupsOutOfOrder(AbstractPackager<?> packager) throws PackagerInterruptedException {
		List<BoxItemGroup> groups = new ArrayList<>();
		for(String id : List.of("a", "b", "c")) {
			groups.add(new BoxItemGroup(id, List.of(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1))));
		}
		Container container = Container.newBuilder().withId("cube").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		PackagerSession session = packager.createSession(new PackagerInput(null, groups, List.of(new ContainerItem(container, 3)), 3, Order.NONE), () -> false);

		Stack stack = new Stack();
		stack.add(new Placement(groups.get(1).get(0).getBox().getStackValue(0), 0, 0, 0, 0));
		session.accept(new DefaultIntermediatePackagerResult(session.getContainerItem(0), stack));
		assertThat(session.countRemainingBoxItemGroups()).isEqualTo(2);

		List<String> packed = new ArrayList<>();
		for(int i = 0; i < 2; i++) {
			IntermediatePackagerResult result = session.attempt(0, null, false);
			packed.add(result.getStack().getPlacements().get(0).getStackValue().getBox().getId());
			session.accept(result);
		}
		assertThat(packed).containsExactly("a", "c");
		assertThat(session.countRemainingBoxes()).isZero();
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

	/**
	 * Three groups of three different unit cubes, each container holds one group. There are enough permutations to split
	 * them between the threads, also after the first groups are accepted:
	 *
	 * <pre>
	 *   [a1][a2][a3]   [b1][b2][b3]   [c1][c2][c3]
	 * </pre>
	 */
	@Test
	public void parallelBruteForcePacksLargerGroupsInThreads() {
		List<BoxItemGroup> groups = new ArrayList<>();
		for(String id : List.of("a", "b", "c")) {
			List<BoxItem> boxItems = new ArrayList<>();
			for(int i = 1; i <= 3; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			groups.add(new BoxItemGroup(id, boxItems));
		}
		Container container = Container.newBuilder().withId("row").withSize(3, 1, 1).withMaxLoadWeight(3).build();

		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 3)))
					.withBoxItemGroups(groups)
					.withMaxContainerCount(3)
					.withInterruptDuration(10_000)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(3);
			for(Container packed : result.getContainers()) {
				// one group per container
				assertThat(packed.getStack().getPlacements())
						.extracting(p -> p.getStackValue().getBox().getId().substring(0, 1))
						.containsOnly(packed.getStack().getPlacements().get(0).getStackValue().getBox().getId().substring(0, 1))
						.hasSize(3);
			}
		}
	}
}
