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
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

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
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
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
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
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
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
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
		stack.add(new Placement(groups.get(1).get(0), groups.get(1).get(0).getBox().getStackValue(0), 0, 0, 0, 0));
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

		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
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

	@Test
	public void bruteForceFillsTheFirstContainer() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertFillsTheFirstContainer(packager);
		}
	}

	@Test
	public void fastBruteForceFillsTheFirstContainer() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertFillsTheFirstContainer(packager);
		}
	}

	@Test
	public void parallelBruteForceFillsTheFirstContainer() {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertFillsTheFirstContainer(packager);
		}
	}

	/**
	 * Each container gets the best order of the remaining groups. Containers with room for 4 unit cubes; groups a
	 * (2 cubes), b (3) and c (2):
	 *
	 * <pre>
	 *   input order:    [a a . .]  [b b b .]  [c c . .]     3 containers
	 *   best orders:    [a a c c]  [b b b .]                2 containers
	 * </pre>
	 */
	private static void assertFillsTheFirstContainer(AbstractPackager<?> packager) {
		List<BoxItemGroup> groups = new ArrayList<>();
		int[] counts = {2, 3, 2};
		String[] ids = {"a", "b", "c"};
		for(int g = 0; g < ids.length; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			for(int i = 0; i < counts[g]; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId(ids[g] + i).withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			groups.add(new BoxItemGroup(ids[g], boxItems));
		}
		Container container = Container.newBuilder().withId("row").withSize(4, 1, 1).withMaxLoadWeight(100).build();

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 3)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(3)
				.withInterruptDuration(10_000)
				.build();

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
		assertThat(result.get(0).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId().substring(0, 1)).containsOnly("a", "c");
		assertThat(result.get(1).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId().substring(0, 1)).containsOnly("b");
	}

	@Test
	public void bruteForceSearchesGroupOrders() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertSearchesGroupOrders(packager);
		}
	}

	@Test
	public void fastBruteForceSearchesGroupOrders() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertSearchesGroupOrders(packager);
		}
	}

	@Test
	public void parallelBruteForceSearchesGroupOrders() {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertSearchesGroupOrders(packager);
		}
	}

	/**
	 * The group orders are searched: a container is not closed by the first group which does not fit. Containers with
	 * room for 5 unit cubes; groups a, b (3 cubes) and c, d (2):
	 *
	 * <pre>
	 *   largest first:        [a a a . .]  [b b b c c]  [d d . . .]     3 containers
	 *   group orders searched:[a a a c c]  [b b b d d]                  2 containers
	 * </pre>
	 */
	private static void assertSearchesGroupOrders(AbstractPackager<?> packager) {
		List<BoxItemGroup> groups = new ArrayList<>();
		int[] counts = {3, 3, 2, 2};
		String[] ids = {"a", "b", "c", "d"};
		for(int g = 0; g < ids.length; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			for(int i = 0; i < counts[g]; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId(ids[g] + i).withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			groups.add(new BoxItemGroup(ids[g], boxItems));
		}
		List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("row").withSize(5, 1, 1).withMaxLoadWeight(100).build(), 4));

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItemGroups(groups)
				.withMaxContainerCount(4)
				.withInterruptDuration(10_000)
				.build();

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
		try (DefaultValidator validator = new DefaultValidator()) {
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(4)
					.withBoxItemGroups(groups));
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Results for searched group orders hold any of the remaining groups; they must be accepted correctly, over several
	 * containers.
	 */
	@Test
	public void groupOrderSearchResultsAreValid() throws Exception {
		List<AbstractPackager<?>> packagers = List.of(
				BruteForcePackager.newBuilder().build(),
				FastBruteForcePackager.newBuilder().build(),
				ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		try (DefaultValidator validator = new DefaultValidator()) {
			for (AbstractPackager<?> packager : packagers) {
				int containers = 0;
				for (long seed = 0; seed < 30; seed++) {
					java.util.Random random = new java.util.Random(seed);
					List<BoxItemGroup> groups = new ArrayList<>();
					int index = 0;
					int groupCount = 3 + random.nextInt(3);
					for (int g = 0; g < groupCount; g++) {
						List<BoxItem> boxItems = new ArrayList<>();
						int count = 1 + random.nextInt(2);
						for (int i = 0; i < count; i++) {
							boxItems.add(new BoxItem(Box.newBuilder().withId("b" + index++).withSize(1 + random.nextInt(2), 1 + random.nextInt(2), 1).withRotate2D().withWeight(1).build(), 1));
						}
						groups.add(new BoxItemGroup("g" + g, boxItems));
					}
					List<ContainerItem> containerItems = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(4, 2, 1).withMaxLoadWeight(100).build(), 10));

					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containerItems)
							.withBoxItemGroups(groups)
							.withMaxContainerCount(10)
							.withInterruptDuration(10_000)
							.build();

					assertThat(result.isSuccess()).as("%s seed %d", packager.getClass().getSimpleName(), seed).isTrue();
					containers += result.size();
					PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
							.withContainerItems(containerItems)
							.withMaxContainerCount(10)
							.withBoxItemGroups(groups));
				}
				assertThat(containers).isGreaterThan(30);
			}
		} finally {
			for (AbstractPackager<?> packager : packagers) {
				packager.close();
			}
		}
	}

	/**
	 * The group order search skips the orders which cannot give a better result: the results are the same as when
	 * every order is tried.
	 */
	@Test
	public void skippingGroupOrdersGivesTheSameResults() {
		List<java.util.function.Supplier<AbstractBruteForcePackager>> packagers = List.of(
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build());
		for (java.util.function.Supplier<AbstractBruteForcePackager> supplier : packagers) {
			try (AbstractBruteForcePackager skipping = supplier.get(); AbstractBruteForcePackager everyOrder = supplier.get()) {
				everyOrder.skipGroupOrders = false;
				for (long seed = 0; seed < 30; seed++) {
					for (boolean priorities : new boolean[] { false, true }) {
						assertThat(packGroupOrders(skipping, seed, priorities))
								.as("%s seed %d priorities %s", skipping.getClass().getSimpleName(), seed, priorities)
								.isEqualTo(packGroupOrders(everyOrder, seed, priorities));
					}
				}
			}
		}
	}

	/**
	 * A best result so far (the hint) prunes the orders which cannot load more; a pruned order says nothing about its
	 * first group, so the orders which begin with it are still searched. Container with room for 5 unit cubes, groups
	 * b (a 4 x 1 box), c (3 cubes) and a (2 cubes), largest first, and a hint of volume 4:
	 *
	 * <pre>
	 *   b, c, a:   [b b b b .]   volume 4
	 *   b, a, c:   [b b b b .]   volume 4
	 *   c, b, a:   [c c c . .]   b does not fit after c: at most volume 3, pruned
	 *   c, a, b:   [c c c a a]   volume 5, the best order
	 *   a, b, c:   [a a . . .]   at most volume 2, pruned
	 *   a, c, b:   [a a c c c]   volume 5
	 * </pre>
	 */
	@Test
	public void skippingGroupOrdersWithAHintGivesTheSameResults() throws Exception {
		List<java.util.function.Supplier<AbstractBruteForcePackager>> packagers = List.of(
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		for (java.util.function.Supplier<AbstractBruteForcePackager> supplier : packagers) {
			try (AbstractBruteForcePackager skipping = supplier.get(); AbstractBruteForcePackager everyOrder = supplier.get()) {
				everyOrder.skipGroupOrders = false;
				String expected = packWithHint(everyOrder);
				assertThat(expected).isEqualTo("c0@0 c1@1 c2@2 a0@3 a1@4");
				assertThat(packWithHint(skipping)).as(skipping.getClass().getSimpleName()).isEqualTo(expected);
			}
		}
	}

	/** @return the placements (box id and x) of the first container */
	private static String packWithHint(AbstractPackager<?> packager) throws PackagerInterruptedException {
		Container container = Container.newBuilder().withId("row").withSize(5, 1, 1).withMaxLoadWeight(100).build();

		// the hint: a result of volume 4
		PackagerSession hintSession = packager.createSession(new PackagerInput(null, List.of(createGroup("h", 4, 1)), List.of(new ContainerItem(container, 1)), 1, Order.NONE), () -> false);
		IntermediatePackagerResult hint = hintSession.attempt(0, null, false);
		assertThat(hint.getLoadVolume()).isEqualTo(4);

		List<BoxItemGroup> groups = List.of(createGroup("a", 2, 1), createGroup("b", 1, 4), createGroup("c", 3, 1));
		PackagerSession session = packager.createSession(new PackagerInput(null, groups, List.of(new ContainerItem(container, 3)), 3, Order.NONE), () -> false);
		IntermediatePackagerResult result = session.attempt(0, hint, false);
		StringBuilder builder = new StringBuilder();
		for (Placement placement : result.getStack().getPlacements()) {
			builder.append(placement.getStackValue().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(' ');
		}
		return builder.toString().trim();
	}

	/** @return a group of boxes of length dx (1 x 1 cross section) */
	private static BoxItemGroup createGroup(String id, int count, int dx) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(dx, 1, 1).withWeight(1).build(), 1));
		}
		return new BoxItemGroup(id, boxItems);
	}

	/** @return the placements of each container (box id and position) */
	private static List<String> packGroupOrders(AbstractPackager<?> packager, long seed, boolean priorities) {
		java.util.Random random = new java.util.Random(seed);
		List<BoxItemGroup> groups = new ArrayList<>();
		int index = 0;
		int groupCount = 3 + random.nextInt(3);
		for (int g = 0; g < groupCount; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			int count = 1 + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				boxItems.add(new BoxItem(Box.newBuilder().withId("b" + index++).withSize(1 + random.nextInt(2), 1 + random.nextInt(2), 1).withRotate2D().withWeight(1).build(), 1));
			}
			BoxItemGroup group = new BoxItemGroup("g" + g, boxItems);
			if(priorities) {
				group.withContainerPriority(random.nextInt(2));
			}
			groups.add(group);
		}
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(4, 2, 1).withMaxLoadWeight(100).build(), 10)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(10)
				.withInterruptDuration(10_000)
				.build();
		List<String> containers = new ArrayList<>();
		for (Container container : result.getContainers()) {
			StringBuilder builder = new StringBuilder();
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getStackValue().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY())
						.append(' ').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append(' ');
			}
			containers.add(builder.toString());
		}
		return containers;
	}
}
