package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.ContainerPriorityValidator;
import com.github.skjolber.packing.validator.ExtractionOrderValidator;
import com.github.skjolber.packing.validator.GroupInsertionValidator;
import com.github.skjolber.packing.validator.InsertionOrderValidator;

/**
 * Results respect the extraction order (boxes can be extracted in their order) and the container priority (boxes are
 * in containers in order of their priority), for random orders.
 */
public class OrderingResultsTest {

	private static final int SEEDS = 20;

	private static List<BoxItem> items(Random random, int types, boolean extraction) {
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < types; i++) {
			BoxItem item = new BoxItem(Box.newBuilder().withId("b" + i).withSize(2 + random.nextInt(6), 2 + random.nextInt(6), 1 + random.nextInt(5)).withRotate3D().withWeight(1).build(), 1 + random.nextInt(2));
			if(extraction) {
				item.withExtractionOrder(1 + random.nextInt(3));
			} else {
				item.withContainerPriority(random.nextInt(3));
			}
			items.add(item);
		}
		return items;
	}

	private static List<ContainerItem> containers(int dx, int dy, int dz, ContainerAccess access, int count) {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("c").withSize(dx, dy, dz).withMaxLoadWeight(100_000).withAccess(access).build(), count)
				.build();
	}

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void boxesCanBeExtractedInTheirOrder(ContainerAccess access) {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> BruteForcePackager.newBuilder().build());

		List<String> failures = new ArrayList<>();
		int mixed = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				boolean bruteForce = packager instanceof BruteForcePackager;
				for (long seed = 0; seed < SEEDS; seed++) {
					List<BoxItem> items = items(new Random(seed), bruteForce ? 4 : 8, true);
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(20, 15, 12, access, 4))
							.withBoxItems(items)
							.withMaxContainerCount(4)
							.withInterruptDuration(5_000)
							.build();
					mixed += check(packager, seed, result, failures);
				}
			}
		}
		assertThat(mixed).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void groupsCanBeExtractedInTheirOrder(ContainerAccess access) {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build());

		List<String> failures = new ArrayList<>();
		int mixed = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItemGroup> groups = new ArrayList<>();
					for (int g = 0; g < 4; g++) {
						List<BoxItem> items = new ArrayList<>();
						for (int i = 0; i < 2; i++) {
							items.add(new BoxItem(Box.newBuilder().withId("g" + g + "-" + i).withSize(2 + random.nextInt(6), 2 + random.nextInt(6), 1 + random.nextInt(5)).withRotate3D().withWeight(1).build(), 1 + random.nextInt(2)));
						}
						groups.add(new BoxItemGroup("g" + g, items).withExtractionOrder(1 + random.nextInt(3)));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(20, 15, 12, access, 4))
							.withBoxItemGroups(groups)
							.withMaxContainerCount(4)
							.build();
					mixed += check(packager, seed, result, failures);
				}
			}
		}
		assertThat(mixed).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}

	//
	//  side view (z up), door at x = 30; the number is the extraction order (stop)
	//
	//  12 +------------------------------+
	//     |    3     |    2     |   1    |   door ->   the boxes for the last stop are placed first,
	//   0 +------------------------------+             furthest from the door
	//     0                             30  x
	//
	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void boxesForTheLastStopsArePlacedFirst(ContainerAccess access) {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build());

		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (boolean stops : new boolean[] {false, true}) {
					Random random = new Random(5);
					List<BoxItem> items = new ArrayList<>();
					for (int stop = 1; stop <= 3; stop++) {
						for (int i = 0; i < 3; i++) {
							BoxItem item = new BoxItem(Box.newBuilder().withId("stop" + stop + "-" + i).withSize(3 + random.nextInt(5), 3 + random.nextInt(5), 2 + random.nextInt(4)).withRotate3D().withWeight(1).build(), 2);
							if(stops) {
								item.withExtractionOrder(stop);
							}
							items.add(item);
						}
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(30, 15, 12, access, 1))
							.withBoxItems(items)
							.withMaxContainerCount(1)
							.build();
					assertThat(result.isSuccess()).as(packager.getClass().getSimpleName() + (stops ? " with" : " without") + " stops").isTrue();
					List<String> failures = new ArrayList<>();
					check(packager, 5, result, failures);
					assertThat(failures).isEmpty();
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void boxesOfAGroupAreInsertedTogether(ContainerAccess access) {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> BruteForcePackager.newBuilder().build());

		List<String> failures = new ArrayList<>();
		int shared = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				boolean bruteForce = packager instanceof BruteForcePackager || packager instanceof FastBruteForcePackager;
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItemGroup> groups = new ArrayList<>();
					for (int g = 0; g < (bruteForce ? 3 : 5); g++) {
						List<BoxItem> items = new ArrayList<>();
						for (int i = 0; i < 2; i++) {
							Box.Builder box = Box.newBuilder().withId("g" + g + "-" + i).withSize(2 + random.nextInt(5), 2 + random.nextInt(5), 1 + random.nextInt(4)).withWeight(1);
							// brute force: few rotations, as the search is exponential
							items.add(new BoxItem((bruteForce ? box.withRotate2D() : box.withRotate3D()).build(), bruteForce ? 1 : 1 + random.nextInt(2)));
						}
						groups.add(new BoxItemGroup("g" + g, items));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(12, 10, 8, access, 5))
							.withBoxItemGroups(groups)
							.withMaxContainerCount(5)
							.withInterruptDuration(5_000)
							.build();
					String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
					if(result.isSuccess() && !result.isInsertionOrder()) {
						failures.add(name + "not in insertion order");
					}
					for (Container container : result.getContainers()) {
						List<ValidatorResultReason> reasons = new ArrayList<>();
						if(!new InsertionOrderValidator().validate(container, reasons) || !new GroupInsertionValidator().validate(container, reasons)) {
							failures.add(name + reasons.get(0).getMessage());
						}
						List<Placement> placements = container.getStack().getPlacements();
						if(!placements.get(0).getBoxItem().getGroupKey().equals(placements.get(placements.size() - 1).getBoxItem().getGroupKey())) {
							shared++;
						}
					}
				}
			}
		}
		assertThat(shared).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}

	/**
	 * @return the number of containers with boxes of several extraction orders
	 */
	private static int check(AbstractPackager<?> packager, long seed, PackagerResult result, List<String> failures) {
		int mixed = 0;
		String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
		if(result.isSuccess() && !result.isInsertionOrder()) {
			failures.add(name + "not in insertion order");
		}
		for (Container container : result.getContainers()) {
			List<ValidatorResultReason> reasons = new ArrayList<>();
			if(!new InsertionOrderValidator().validate(container, reasons) || !new ExtractionOrderValidator().validate(container, reasons)) {
				failures.add(name + reasons.get(0).getMessage());
			}
			// the boxes extracted last are inserted first
			List<Placement> placements = container.getStack().getPlacements();
			for (int i = 1; i < placements.size(); i++) {
				if(placements.get(i).getBoxItem().getExtractionOrder() > placements.get(i - 1).getBoxItem().getExtractionOrder()) {
					failures.add(name + "not in descending extraction order");
					break;
				}
			}
			if(placements.get(0).getBoxItem().getExtractionOrder() != placements.get(placements.size() - 1).getBoxItem().getExtractionOrder()) {
				mixed++;
			}
		}
		return mixed;
	}

	@Test
	public void boxesAreInContainersInOrderOfPriority() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build());

		ContainerPriorityValidator validator = new ContainerPriorityValidator();
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(10, 10, 8, ContainerAccess.ANY, 8))
							.withBoxItems(items(new Random(seed), 10, false))
							.withMaxContainerCount(8)
							.build();
					if(result.size() > 1) {
						multiple++;
					}
					List<ValidatorResultReason> reasons = new ArrayList<>();
					if(!validator.validate(result.getContainers(), reasons)) {
						failures.add(packager.getClass().getSimpleName() + " seed " + seed + ": " + reasons.get(0).getMessage());
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}

	@Test
	public void groupsAreInContainersInOrderOfPriority() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build());

		ContainerPriorityValidator validator = new ContainerPriorityValidator();
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItemGroup> groups = new ArrayList<>();
					for (int g = 0; g < 6; g++) {
						List<BoxItem> items = new ArrayList<>();
						for (int i = 0; i < 2; i++) {
							items.add(new BoxItem(Box.newBuilder().withId("g" + g + "-" + i).withSize(2 + random.nextInt(5), 2 + random.nextInt(5), 1 + random.nextInt(4)).withRotate3D().withWeight(1).build(), 1 + random.nextInt(2)));
						}
						groups.add(new BoxItemGroup("g" + g, items).withContainerPriority(random.nextInt(3)));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(10, 10, 8, ContainerAccess.ANY, 8))
							.withBoxItemGroups(groups)
							.withMaxContainerCount(8)
							.build();
					if(result.size() > 1) {
						multiple++;
					}
					List<ValidatorResultReason> reasons = new ArrayList<>();
					if(!validator.validate(result.getContainers(), reasons)) {
						failures.add(packager.getClass().getSimpleName() + " seed " + seed + ": " + reasons.get(0).getMessage());
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS / 2);
		assertThat(failures).isEmpty();
	}

	//
	//  containers 2 x 1 x 1; the number is the container priority
	//
	//    first          second
	//  +---+---+      +---+---+
	//  | 0 | 0 |      | 0 | 1 |   the box of priority 1 waits until all boxes of priority 0 are placed,
	//  +---+---+      +---+---+   although it is the largest
	//
	@Test
	public void boxesOfTheNextPriorityWaitForTheBoxesBeforeThem() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build());

		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				List<BoxItem> items = List.of(
						new BoxItem(Box.newBuilder().withId("late").withSize(1, 1, 1).withWeight(5).build(), 1).withContainerPriority(1),
						new BoxItem(Box.newBuilder().withId("early").withSize(1, 1, 1).withWeight(1).build(), 3));
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers(2, 1, 1, ContainerAccess.ANY, 2))
						.withBoxItems(items)
						.withMaxContainerCount(2)
						.build();

				String name = packager.getClass().getSimpleName();
				assertThat(result.isSuccess()).as(name).isTrue();
				assertThat(result.getContainers()).as(name).hasSize(2);
				for (Placement placement : result.getContainers().get(0).getStack().getPlacements()) {
					assertThat(placement.getBoxItem().getBox().getId()).as(name).isEqualTo("early");
				}
				assertThat(result.getContainers().get(1).getStack().getPlacements()).as(name).anyMatch(placement -> placement.getBoxItem().getBox().getId().equals("late"));
			}
		}
	}

	@Test
	public void containerPrioritiesMustNotDecreaseInTheBoxItemOrder() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			List<ContainerItem> containers = containers(2, 1, 1, ContainerAccess.ANY, 2);
			List<BoxItem> decreasing = List.of(
					new BoxItem(Box.newBuilder().withId("late").withSize(1, 1, 1).withWeight(1).build(), 1).withContainerPriority(1),
					new BoxItem(Box.newBuilder().withId("early").withSize(1, 1, 1).withWeight(1).build(), 3));
			assertThat(packager.getUnsupportedReason(new PackagerInput(decreasing, null, containers, 2, Order.CHRONOLOGICAL))).contains("decrease");
			// without a box item order, the items are sorted
			assertThat(packager.getUnsupportedReason(new PackagerInput(decreasing, null, containers, 2, Order.NONE))).isNull();

			//
			//    first          second
			//  +---+---+      +---+---+
			//  | 0 | 0 |      | 0 | 1 |   in the box item order
			//  +---+---+      +---+---+
			//
			List<BoxItem> increasing = List.of(
					new BoxItem(Box.newBuilder().withId("early").withSize(1, 1, 1).withWeight(1).build(), 3),
					new BoxItem(Box.newBuilder().withId("late").withSize(1, 1, 1).withWeight(1).build(), 1).withContainerPriority(1));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(increasing)
					.withOrder(Order.CHRONOLOGICAL)
					.withMaxContainerCount(2)
					.build();
			assertThat(result.isSuccess()).isTrue();
			assertThat(new ContainerPriorityValidator().validate(result.getContainers(), new ArrayList<>())).isTrue();
		}
	}

	@Test
	public void fastBruteForceDoesNotSupportSkipping() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			List<BoxItem> items = smallItems(new Random(1), 3);
			assertThat(packager.getUnsupportedReason(new PackagerInput(items, null, containers(10, 10, 8, ContainerAccess.ANY, 2), 2, Order.CHRONOLOGICAL_ALLOW_SKIPPING))).contains("CHRONOLOGICAL_ALLOW_SKIPPING");
		}
	}

	//
	//  container 2 x 1 x 1; boxes in order a (1 x 1 x 1), b (2 x 1 x 1), c (1 x 1 x 1). With skipping, b waits for
	//  the next container, and c is placed after a; without skipping, the container closes at b.
	//
	//   container 1   container 2
	//     [a][c]        [ b  ]
	//
	@Test
	public void bruteForceSkipsBoxesWhichDoNotFit() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> BruteForcePackager.newBuilder().build(),
				() -> ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				List<BoxItem> items = List.of(
						new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("b").withSize(2, 1, 1).withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("c").withSize(1, 1, 1).withWeight(1).build(), 1));
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers(2, 1, 1, ContainerAccess.ANY, 2))
						.withBoxItems(items)
						.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
						.withMaxContainerCount(2)
						.withInterruptDuration(10_000)
						.build();

				assertThat(result.isSuccess()).isTrue();
				assertThat(result.getContainers()).hasSize(2);
				assertThat(result.get(0).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("a", "c");
				assertThat(result.get(1).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("b");
			}
		}
	}

	private static List<Supplier<AbstractPackager<?>>> bruteForcePackagers() {
		return List.of(
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				// a box item order or container priorities are searched on one thread
				() -> ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
	}

	/** Few boxes with few rotations: the brute-force search is exponential. */
	private static List<BoxItem> smallItems(Random random, int count) {
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			items.add(new BoxItem(Box.newBuilder().withId("b" + i).withSize(3 + random.nextInt(5), 3 + random.nextInt(5), 2 + random.nextInt(4)).withRotate2D().withWeight(1).build(), 1));
		}
		return items;
	}

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void bruteForcePacksInTheBoxItemOrder(ContainerAccess access) {
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : bruteForcePackagers()) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					List<BoxItem> items = smallItems(new Random(seed), 6);
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(9, 8, 6, access, 6))
							.withBoxItems(items)
							.withOrder(Order.CHRONOLOGICAL)
							.withMaxContainerCount(6)
							.withInterruptDuration(5_000)
							.build();
					String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
					if(!result.isSuccess()) {
						failures.add(name + "not packed");
						continue;
					}
					if(result.size() > 1) {
						multiple++;
					}
					// the boxes, container by container, in the box item order
					List<String> packed = new ArrayList<>();
					for (Container container : result.getContainers()) {
						List<ValidatorResultReason> reasons = new ArrayList<>();
						if(!new InsertionOrderValidator().validate(container, reasons)) {
							failures.add(name + reasons.get(0).getMessage());
						}
						for (Placement placement : container.getStack().getPlacements()) {
							packed.add(placement.getStackValue().getBox().getId());
						}
					}
					List<String> expected = new ArrayList<>();
					for (BoxItem item : items) {
						expected.add(item.getBox().getId());
					}
					if(!packed.equals(expected)) {
						failures.add(name + "packed " + packed + ", expected " + expected);
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS / 2);
		assertThat(failures).isEmpty();
	}

	@Test
	public void bruteForcePacksGroupsInTheBoxItemOrder() {
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : bruteForcePackagers()) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					List<BoxItem> items = smallItems(new Random(seed), 6);
					List<BoxItemGroup> groups = new ArrayList<>();
					for (int g = 0; g < 3; g++) {
						groups.add(new BoxItemGroup("g" + g, new ArrayList<>(items.subList(g * 2, g * 2 + 2))));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(12, 10, 7, ContainerAccess.FRONT, 6))
							.withBoxItemGroups(groups)
							.withOrder(Order.CHRONOLOGICAL)
							.withMaxContainerCount(6)
							.withInterruptDuration(5_000)
							.build();
					String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
					if(!result.isSuccess()) {
						failures.add(name + "not packed");
						continue;
					}
					if(result.size() > 1) {
						multiple++;
					}
					List<String> packed = new ArrayList<>();
					for (Container container : result.getContainers()) {
						List<ValidatorResultReason> reasons = new ArrayList<>();
						if(!new InsertionOrderValidator().validate(container, reasons)) {
							failures.add(name + reasons.get(0).getMessage());
						}
						for (Placement placement : container.getStack().getPlacements()) {
							packed.add(placement.getStackValue().getBox().getId());
						}
					}
					List<String> expected = new ArrayList<>();
					for (BoxItem item : items) {
						expected.add(item.getBox().getId());
					}
					if(!packed.equals(expected)) {
						failures.add(name + "packed " + packed + ", expected " + expected);
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS / 4);
		assertThat(failures).isEmpty();
	}

	/**
	 * With a box item order, brute force searches all rotations and points of the boxes in their order: it places at
	 * least as many boxes in a container as fast brute force (which takes the best point for each box), and the same
	 * number when attempting the container again.
	 */
	@Test
	public void bruteForceInOrderSearchesAllRotationsAndPoints() throws Exception {
		try (BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build(); FastBruteForcePackager fast = FastBruteForcePackager.newBuilder().build()) {
			int more = 0;
			for (long seed = 0; seed < SEEDS * 2; seed++) {
				Random random = new Random(seed);
				List<BoxItem> items = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					Box.Builder box = Box.newBuilder().withId("b" + i).withSize(2 + random.nextInt(5), 2 + random.nextInt(5), 1 + random.nextInt(4)).withWeight(1);
					items.add(new BoxItem((random.nextBoolean() ? box.withRotate3D() : box.withRotate2D()).build(), 1));
				}
				List<ContainerItem> containers = containers(10, 8, 6, ContainerAccess.ANY, 6);

				PackagerSession session = bruteForce.createSession(new PackagerInput(items, null, containers, 6, Order.CHRONOLOGICAL), () -> false);
				int placed = session.attempt(0, null, false).getStack().size();
				assertThat(session.attempt(0, null, false).getStack().size()).as("seed %d, again", seed).isEqualTo(placed);

				PackagerSession fastSession = fast.createSession(new PackagerInput(items, null, containers, 6, Order.CHRONOLOGICAL), () -> false);
				int fastPlaced = fastSession.attempt(0, null, false).getStack().size();
				assertThat(placed).as("seed %d", seed).isGreaterThanOrEqualTo(fastPlaced);
				if(placed > fastPlaced) {
					more++;
				}
			}
			assertThat(more).isGreaterThan(0);
		}
	}

	//
	//  containers: small 1 x 1 x 1 (two), big 2 x 1 x 1 (one); boxes in order a, B, c
	//
	//  small: [a]     big: [B  ]     small: [c]
	//
	//  the small container cannot take c after a, as B, which comes before c, does not fit it
	//
	@Test
	public void bruteForceDoesNotPackBoxesAfterABoxWhichDoesNotFitTheContainer() {
		for (Supplier<AbstractPackager<?>> supplier : bruteForcePackagers()) {
			try (AbstractPackager<?> packager = supplier.get()) {
				List<BoxItem> items = List.of(
						new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("B").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("c").withSize(1, 1, 1).withWeight(1).build(), 1));
				List<ContainerItem> containers = ContainerItem.newListBuilder()
						.withContainer(Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(100).build(), 2)
						.withContainer(Container.newBuilder().withId("big").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1)
						.build();
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers)
						.withBoxItems(items)
						.withOrder(Order.CHRONOLOGICAL)
						.withMaxContainerCount(3)
						.withInterruptDuration(5_000)
						.build();

				String name = packager.getClass().getSimpleName();
				assertThat(result.isSuccess()).as(name).isTrue();
				List<String> packed = new ArrayList<>();
				for (Container container : result.getContainers()) {
					for (Placement placement : container.getStack().getPlacements()) {
						packed.add(placement.getStackValue().getBox().getId());
					}
				}
				assertThat(packed).as(name).containsExactly("a", "B", "c");
			}
		}
	}

	@Test
	public void bruteForceBoxesAreInContainersInOrderOfPriority() {
		ContainerPriorityValidator validator = new ContainerPriorityValidator();
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : bruteForcePackagers()) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItem> items = smallItems(random, 6);
					for (BoxItem item : items) {
						item.withContainerPriority(random.nextInt(3));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(9, 8, 6, ContainerAccess.ANY, 6))
							.withBoxItems(items)
							.withMaxContainerCount(6)
							.withInterruptDuration(5_000)
							.build();
					String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
					if(!result.isSuccess()) {
						failures.add(name + "not packed");
						continue;
					}
					if(result.size() > 1) {
						multiple++;
					}
					List<ValidatorResultReason> reasons = new ArrayList<>();
					if(!validator.validate(result.getContainers(), reasons)) {
						failures.add(name + reasons.get(0).getMessage());
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS / 2);
		assertThat(failures).isEmpty();
	}

	@Test
	public void bruteForceGroupsAreInContainersInOrderOfPriority() {
		ContainerPriorityValidator validator = new ContainerPriorityValidator();
		List<String> failures = new ArrayList<>();
		int multiple = 0;
		for (Supplier<AbstractPackager<?>> supplier : bruteForcePackagers()) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItem> items = smallItems(random, 6);
					List<BoxItemGroup> groups = new ArrayList<>();
					for (int g = 0; g < 3; g++) {
						groups.add(new BoxItemGroup("g" + g, new ArrayList<>(items.subList(g * 2, g * 2 + 2))).withContainerPriority(random.nextInt(3)));
					}
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containers(12, 10, 7, ContainerAccess.ANY, 6))
							.withBoxItemGroups(groups)
							.withMaxContainerCount(6)
							.withInterruptDuration(5_000)
							.build();
					String name = packager.getClass().getSimpleName() + " seed " + seed + ": ";
					if(!result.isSuccess()) {
						failures.add(name + "not packed");
						continue;
					}
					if(result.size() > 1) {
						multiple++;
					}
					List<ValidatorResultReason> reasons = new ArrayList<>();
					if(!validator.validate(result.getContainers(), reasons)) {
						failures.add(name + reasons.get(0).getMessage());
					}
				}
			}
		}
		assertThat(multiple).isGreaterThan(SEEDS / 2);
		assertThat(failures).isEmpty();
	}
}
