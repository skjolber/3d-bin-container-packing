package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.cost.LinearBucketWeightContainerCostCalculator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.strategy.BruteForceContainerStrategy;
import com.github.skjolber.packing.packer.strategy.ContainerResult;
import com.github.skjolber.packing.packer.strategy.ContainerStrategy;
import com.github.skjolber.packing.packer.strategy.LowestCostContainersComparator;
import com.github.skjolber.packing.packer.strategy.LowestCostControls;

class ContainerCostPackingTest {

	@Test
	void costAwareStrategyOwnsCostCalculator() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		AtomicInteger estimates = new AtomicInteger();
		EstimatingContainerItemsCostCalculator delegate = new EstimatingContainerItemsCostCalculator();
		ContainerItemsCostCalculator calculator = new ContainerItemsCostCalculator() {
			@Override
			public long getMinimumCost(ContainerItemsCalculator containers, List<BoxItem> boxes, int maxCount) {
				estimates.incrementAndGet();
				return delegate.getMinimumCost(containers, boxes, maxCount);
			}

			@Override
			public long getGroupMinimumCost(ContainerItemsCalculator containers, List<BoxItemGroup> groups, int maxCount) {
				estimates.incrementAndGet();
				return delegate.getGroupMinimumCost(containers, groups, maxCount);
			}
		};
		try {
			useStrategyFactory(packager,
					() -> new BruteForceContainerStrategy(new LowestCostControls(calculator)));
			PackagerResult result = packager.newResultBuilder().withContainerItems(planContainers())
					.withMaxContainerCount(2).withBoxItems(twoBoxes())
					.build();
			assertThat(result.isSuccess()).isTrue();
			assertThat(estimates).hasPositiveValue();
		} finally {
			packager.close();
		}
	}

	@Test
	void packagerStrategyFactoryReceivesContainerCostStatus() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<Boolean> costStatuses = new ArrayList<>();
			packager.setContainerPackingStrategyFactory((calculator, boxes, groups) -> {
				costStatuses.add(calculator.hasCost());
				return (interrupt, adapter) -> new ContainerResult(0, List.of());
			});

			packager.newResultBuilder().withContainerItems(List.of(new ContainerItem(container("plain"), 1)))
					.withBoxItems(boxItem()).build();
			packager.newResultBuilder().withContainerItems(costedContainers())
					.withBoxItems(boxItem()).build();

			assertThat(costStatuses).containsExactly(false, true);
		} finally {
			packager.close();
		}
	}

	@Test
	void groupCountLimitsBruteForceContainerSearchDepth() {
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(container("group"), 3).build();
		ContainerStrategy strategy = (interrupt, adapter) -> {
			assertThat(adapter.countRemainingBoxes()).isEqualTo(2);
			assertThat(adapter.getRemainingBoxItemGroups()).hasSize(1);
			assertThat(adapter.getContainerItemsCalculator().getContainerCount()).isEqualTo(1);
			assertThat(adapter.getMaxContainerCount()).isEqualTo(1);
			return new BruteForceContainerStrategy().pack(interrupt, adapter);
		};
		PlainPackager plain = PlainPackager.newBuilder().build();
		BruteForcePackager brute = BruteForcePackager.newBuilder().build();
		try {
			useStrategy(plain, strategy);
			useStrategy(brute, strategy);
			assertThat(plain.newResultBuilder().withContainerItems(containers)
					.withMaxContainerCount(1000).withBoxItems(new BoxItemGroup("pair", List.of(twoBoxes())))
					.build().isSuccess()).isTrue();
			assertThat(brute.newResultBuilder().withContainerItems(containers)
					.withMaxContainerCount(1000).withBoxItems(new BoxItemGroup("pair", List.of(twoBoxes())))
					.build().isSuccess()).isTrue();
		} finally {
			plain.close();
			brute.close();
		}
	}

	@Test
	void everyPackagerAdapterCanBeRecreatedRepeatedly() {
		List<ContainerItem> containerItems = ContainerItem.newListBuilder()
				.withContainer(container("single"), 1).build();
		PlainPackager plain = PlainPackager.newBuilder().build();
		LargestAreaFitFirstPackager laff = LargestAreaFitFirstPackager.newBuilder().build();
		BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build();
		FastBruteForcePackager fastBruteForce = FastBruteForcePackager.newBuilder().build();
		ParallelBoxItemBruteForcePackager parallel = ParallelBoxItemBruteForcePackager.newBuilder()
				.withThreads(2).withParallelizationCount(2).build();
		try {
			ContainerStrategy strategy = this::packFromRecreatedAdapter;
			useStrategy(plain, strategy);
			useStrategy(laff, strategy);
			useStrategy(bruteForce, strategy);
			useStrategy(fastBruteForce, strategy);
			useStrategy(parallel, strategy);
			assertThat(plain.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(boxItem()).build().isSuccess()).isTrue();
			assertThat(plain.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(new BoxItemGroup("group", List.of(boxItem()))).build().isSuccess()).isTrue();
			assertThat(laff.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(boxItem()).build().isSuccess()).isTrue();
			assertThat(laff.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(new BoxItemGroup("group", List.of(boxItem()))).build().isSuccess()).isTrue();
			assertThat(bruteForce.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(boxItem()).build().isSuccess()).isTrue();
			assertThat(bruteForce.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(new BoxItemGroup("group", List.of(boxItem()))).build().isSuccess()).isTrue();
			assertThat(fastBruteForce.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(boxItem()).build().isSuccess()).isTrue();
			assertThat(fastBruteForce.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(new BoxItemGroup("group", List.of(boxItem()))).build().isSuccess()).isTrue();
			assertThat(parallel.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(boxItem()).build().isSuccess()).isTrue();
			assertThat(parallel.newResultBuilder().withContainerItems(containerItems)
					.withBoxItems(new BoxItemGroup("group", List.of(boxItem()))).build().isSuccess()).isTrue();
		} finally {
			plain.close();
			laff.close();
			bruteForce.close();
			fastBruteForce.close();
			parallel.close();
		}
	}

	private ContainerResult packFromRecreatedAdapter(PackagerInterruptSupplier interrupt,
			PackagerAdapter adapter) throws PackagerInterruptedException {
		assertThat(adapter.countRemainingBoxes()).isEqualTo(1);
		assertThat(adapter.getContainerItem(0).getCount()).isEqualTo(1);
		Container firstPacking = adapter.accept(adapter.attempt(0, null, true));
		assertThat(adapter.countRemainingBoxes()).isZero();
		adapter.reset();
		assertThat(adapter.countRemainingBoxes()).isEqualTo(1);
		assertThat(adapter.getContainerItem(0).getCount()).isEqualTo(1);
		assertThat(firstPacking.getStack().size()).isEqualTo(1);
		adapter.accept(adapter.attempt(0, null, true));
		assertThat(adapter.countRemainingBoxes()).isZero();

		PackagerAdapter first = adapter.fresh();
		assertThat(first.countRemainingBoxes()).isEqualTo(1);
		first.accept(first.attempt(0, null, true));
		assertThat(first.countRemainingBoxes()).isZero();

		PackagerAdapter second = first.fresh();
		assertThat(second.countRemainingBoxes()).isEqualTo(1);
		assertThat(second.getContainerItem(0).getCount()).isEqualTo(1);
		Container packed = second.accept(second.attempt(0, null, true));
		return new ContainerResult(second.getContainerItemsCalculator().getCost(), List.of(packed));
	}

	@Test
	void resetRestoresPartiallyConsumedBruteForceAdapters() {
		List<ContainerItem> containerItems = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build(), 2)
				.build();
		BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build();
		FastBruteForcePackager fastBruteForce = FastBruteForcePackager.newBuilder().build();
		ParallelBoxItemBruteForcePackager parallel = ParallelBoxItemBruteForcePackager.newBuilder()
				.withThreads(2).withParallelizationCount(2).build();
		try {
			ContainerStrategy strategy = this::packAfterPartialReset;
			useStrategy(bruteForce, strategy);
			useStrategy(fastBruteForce, strategy);
			useStrategy(parallel, strategy);
			assertThat(bruteForce.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().isSuccess()).isTrue();
			assertThat(fastBruteForce.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().isSuccess()).isTrue();
			assertThat(parallel.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().isSuccess()).isTrue();
			assertThat(bruteForce.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(new BoxItemGroup("first", List.of(boxItem())), new BoxItemGroup("second", List.of(boxItem())))
					.build().isSuccess()).isTrue();
			assertThat(fastBruteForce.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(new BoxItemGroup("first", List.of(boxItem())), new BoxItemGroup("second", List.of(boxItem())))
					.build().isSuccess()).isTrue();
			assertThat(parallel.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(2)
					.withBoxItems(new BoxItemGroup("first", List.of(boxItem())), new BoxItemGroup("second", List.of(boxItem())))
					.build().isSuccess()).isTrue();
		} finally {
			bruteForce.close();
			fastBruteForce.close();
			parallel.close();
		}
	}

	private ContainerResult packAfterPartialReset(PackagerInterruptSupplier interrupt,
			PackagerAdapter adapter) throws PackagerInterruptedException {
		assertThat(adapter.countRemainingBoxes()).isEqualTo(2);
		Container beforeReset = adapter.accept(adapter.attempt(0, null, false));
		assertThat(beforeReset.getStack().size()).isEqualTo(1);
		assertThat(adapter.countRemainingBoxes()).isEqualTo(1);

		adapter.reset();
		assertThat(adapter.countRemainingBoxes()).isEqualTo(2);
		assertThat(adapter.getContainerItem(0).getCount()).isEqualTo(2);
		Container first = adapter.accept(adapter.attempt(0, null, false));
		Container second = adapter.accept(adapter.attempt(0, null, true));
		assertThat(adapter.countRemainingBoxes()).isZero();
		assertThat(beforeReset.getStack().size()).isEqualTo(1);
		return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), List.of(first, second));
	}

	@Test
	void costPlannerChoosesLowestCostContainer() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(costedContainers())
					.withBoxItems(boxItem())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).singleElement()
					.extracting(Container::getId).isEqualTo("cheap");
		} finally {
			packager.close();
		}
	}

	@Test
	void costPlannerChoosesTwoSmallContainersOverOneExpensiveContainer() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
		} finally {
			packager.close();
		}
	}

	@Test
	void costPlannerIsUsedByBruteForcePackager() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
		} finally {
			packager.close();
		}
	}

	@Test
	void bruteForceStrategyCanOverrideCostPlanning() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			AtomicInteger comparisons = new AtomicInteger();
			useStrategyFactory(packager, () -> comparisonStrategy((first, second) -> {
						comparisons.incrementAndGet();
						return Boolean.compare(first.get(0).getId().equals("medium"), second.get(0).getId().equals("medium"));
					}));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(costedContainers())
					.withBoxItems(boxItem())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).singleElement()
					.extracting(Container::getId).isEqualTo("medium");
			assertThat(comparisons).hasValue(2);
		} finally {
			packager.close();
		}
	}

	@Test
	void bruteForceStrategyCanCompareTotalContainerCost() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<ContainerItem> containerItems = planContainers();
			Map<String, ContainerCostCalculator> calculators = new HashMap<>();
			for(ContainerItem item : containerItems) {
				calculators.put(item.getContainer().getId(), item.getCostCalculator());
			}
			LowestCostContainersComparator comparator = new LowestCostContainersComparator(container ->
					calculators.get(container.getId()).calculateCost(container.getLoadWeight()));
			useStrategyFactory(packager, () -> comparisonStrategy(comparator));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
		} finally {
			packager.close();
		}
	}

	@Test
	void costAwareBruteForceStrategyFindsCheapestCompleteSequence() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			useStrategyFactory(packager,
					() -> new BruteForceContainerStrategy(new LowestCostControls()));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
		} finally {
			packager.close();
		}
	}

	@Test
	void costAwareBruteForceStrategyPrefersFewerContainersAtEqualCost() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Container small = Container.newBuilder().withId("small")
					.withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container large = Container.newBuilder().withId("large")
					.withSize(2, 1, 1).withMaxLoadWeight(2).build();
			List<ContainerItem> containers = ContainerItem.newListBuilder()
					.withContainer(small, 2, cost(40, 1))
					.withContainer(large, 1, cost(80, 2))
					.build();
			useStrategyFactory(packager,
					() -> new BruteForceContainerStrategy(new LowestCostControls()));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("large");
		} finally {
			packager.close();
		}
	}

	@Test
	void costAwareBruteForceStrategyComparesActualLoadCost() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Container variable = Container.newBuilder().withId("variable")
					.withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container fixed = Container.newBuilder().withId("fixed")
					.withSize(1, 1, 1).withMaxLoadWeight(1).build();
			List<ContainerItem> containers = ContainerItem.newListBuilder()
					.withContainer(variable, 1, new LinearBucketWeightContainerCostCalculator(0, 0, 1, 10, 1, 1, null, 0))
					.withContainer(fixed, 1, cost(5, 1))
					.build();
			useStrategyFactory(packager,
					() -> new BruteForceContainerStrategy(new LowestCostControls()));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(boxItem())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("fixed");
		} finally {
			packager.close();
		}
	}

	@Test
	void costEstimateFollowsForkedAndFreshAdapterInventory() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build();
		try {
			ContainerItemsCostCalculator calculator = new EstimatingContainerItemsCostCalculator();
			ContainerStrategy strategy = (interrupt, adapter) -> {
				assertThat(estimateMinimumCost(calculator, adapter, 2)).isEqualTo(80);
				PackagerAdapter branch = adapter.fork();
				branch.accept(branch.attempt(1, null, false));
				assertThat(estimateMinimumCost(calculator, branch, 1)).isEqualTo(40);
				assertThat(estimateMinimumCost(calculator, adapter, 2)).isEqualTo(80);
				assertThat(estimateMinimumCost(calculator, branch.fresh(), 2)).isEqualTo(80);
				return new BruteForceContainerStrategy(new LowestCostControls(calculator)).pack(interrupt, adapter);
			};
			useStrategy(packager, strategy);
			useStrategy(bruteForce, strategy);
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();
			assertThat(result.isSuccess()).isTrue();
			PackagerResult grouped = bruteForce.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoGroups())
					.build();
			assertThat(grouped.isSuccess()).isTrue();
		} finally {
			packager.close();
			bruteForce.close();
		}
	}

	@Test
	void bruteForceStrategyExploresCompleteContainerSequences() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			AtomicInteger comparisons = new AtomicInteger();
			useStrategyFactory(packager, () -> comparisonStrategy((first, second) -> {
				comparisons.incrementAndGet();
				int compare = Integer.compare(first.size(), second.size());
				if(compare != 0) {
					return compare;
				}
				return Integer.compare(countSmall(first), countSmall(second));
			}));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
			assertThat(comparisons.get()).isGreaterThanOrEqualTo(2);
			for(Container container : result.getContainers()) {
				container.getStack().forEach(placement -> {
					Box placedBox = placement.getStackValue().getBox();
					assertThat(placedBox.getBoxItem().getBox()).isSameAs(placedBox);
				});
			}
		} finally {
			packager.close();
		}
	}

	@Test
	void bruteForceContainerStrategyWorksWithFastBruteForcePackager() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			useStrategyFactory(packager, () -> comparisonStrategy((first, second) ->
					Integer.compare(first.size(), second.size())));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(2);
		} finally {
			packager.close();
		}
	}

	@Test
	void bruteForceContainerStrategyWorksWithGroups() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			useStrategyFactory(packager, () -> comparisonStrategy((first, second) ->
					Integer.compare(first.size(), second.size())));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(planContainers())
					.withMaxContainerCount(2)
					.withBoxItems(new BoxItemGroup("first", List.of(boxItem())),
							new BoxItemGroup("second", List.of(boxItem())))
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(2);
		} finally {
			packager.close();
		}
	}

	@Test
	void bruteForceContainerStrategyForksEachPackagerAdapter() {
		BruteForcePackager brute = BruteForcePackager.newBuilder().build();
		FastBruteForcePackager fast = FastBruteForcePackager.newBuilder().build();
		ParallelBoxItemBruteForcePackager parallel = ParallelBoxItemBruteForcePackager.newBuilder()
				.withThreads(2).withParallelizationCount(2).build();
		LargestAreaFitFirstPackager laff = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Supplier<ContainerStrategy> strategyFactory = () -> comparisonStrategy(
					(first, second) -> Integer.compare(first.size(), second.size()));
			useStrategyFactory(brute, strategyFactory);
			useStrategyFactory(fast, strategyFactory);
			useStrategyFactory(parallel, strategyFactory);
			useStrategyFactory(laff, strategyFactory);
			assertThat(brute.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().getContainers()).hasSize(2);
			assertThat(brute.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoGroups()).build().getContainers()).hasSize(2);
			assertThat(fast.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoGroups()).build().getContainers()).hasSize(2);
			assertThat(parallel.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().getContainers()).hasSize(2);
			assertThat(parallel.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoGroups()).build().getContainers()).hasSize(2);
			assertThat(laff.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoBoxes()).build().getContainers()).hasSize(2);
			assertThat(laff.newResultBuilder().withContainerItems(planContainers()).withMaxContainerCount(2)
					.withBoxItems(twoGroups()).build().getContainers()).hasSize(2);
		} finally {
			brute.close();
			fast.close();
			parallel.close();
			laff.close();
		}
	}

	private BoxItemGroup[] twoGroups() {
		return new BoxItemGroup[] {
				new BoxItemGroup("first", List.of(boxItem())),
				new BoxItemGroup("second", List.of(boxItem()))
		};
	}

	@Test
	void bruteForceStrategySearchesBothContainerOrders() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			Container first = Container.newBuilder().withId("first").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container second = Container.newBuilder().withId("second").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			List<ContainerItem> containers = ContainerItem.newListBuilder()
					.withContainer(first, 1)
					.withContainer(second, 1)
					.build();
			useStrategyFactory(packager, () -> comparisonStrategy((left, right) ->
					Boolean.compare(left.get(0).getId().equals("second"), right.get(0).getId().equals("second"))));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(twoBoxes())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("second", "first");
		} finally {
			packager.close();
		}
	}

	private int countSmall(List<Container> containers) {
		int count = 0;
		for(Container container : containers) {
			if(container.getId().equals("small")) {
				count++;
			}
		}
		return count;
	}

	private static void useStrategy(AbstractPackager<?> packager, ContainerStrategy strategy) {
		packager.setContainerPackingStrategyFactory((calculator, boxes, groups) -> strategy);
	}

	private static void useStrategyFactory(AbstractPackager<?> packager,
			Supplier<? extends ContainerStrategy> strategyFactory) {
		packager.setContainerPackingStrategyFactory((calculator, boxes, groups) -> strategyFactory.get());
	}

	private static BruteForceContainerStrategy comparisonStrategy(Comparator<List<Container>> comparator) {
		return new BruteForceContainerStrategy(new ComparisonControls(comparator));
	}

	private static class ComparisonControls implements BruteForceContainerStrategy.Controls {

		private final Comparator<List<Container>> comparator;
		private ContainerResult best;

		private ComparisonControls(Comparator<List<Container>> comparator) {
			this.comparator = comparator;
		}

		@Override
		public boolean attempt(List<Container> containers, PackagerAdapter state,
				List<Integer> availableContainerIndexes, int selectedContainerIndex) {
			return true;
		}

		@Override
		public boolean result(ContainerResult result) {
			if(best == null || comparator.compare(best.getPackList(), result.getPackList()) < 0) {
				best = result;
			}
			return true;
		}

		@Override
		public ContainerResult getResult() {
			return best;
		}
	}

	@Test
	void controlledContainerBuilderCanAttachCost() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(builder -> builder
							.withContainerItem(container("controlled"), 1)
							.withCostCalculator(cost(10)))
					.withBoxItems(boxItem())
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).singleElement()
					.extracting(Container::getId).isEqualTo("controlled");
		} finally {
			packager.close();
		}
	}

	private List<ContainerItem> costedContainers() {
		return ContainerItem.newListBuilder()
				.withContainer(container("expensive", 10), 1, cost(300, 1_000))
				.withContainer(container("cheap", 20), 1, cost(100, 8_000))
				.withContainer(container("medium", 15), 1, cost(200, 3_375))
				.build();
	}

	private static long estimateMinimumCost(ContainerItemsCostCalculator calculator,
			PackagerAdapter adapter, int maxCount) {
		if(adapter.getRemainingBoxItemGroups() != null) {
			return calculator.getGroupMinimumCost(adapter.getContainerItemsCalculator(),
					adapter.getRemainingBoxItemGroups(), maxCount);
		}
		return calculator.getMinimumCost(adapter.getContainerItemsCalculator(),
				adapter.getRemainingBoxItems(), maxCount);
	}

	private List<ContainerItem> planContainers() {
		Container large = Container.newBuilder()
				.withId("large")
				.withSize(2, 1, 1)
				.withMaxLoadWeight(2)
				.build();
		Container small = Container.newBuilder()
				.withId("small")
				.withSize(1, 1, 1)
				.withMaxLoadWeight(1)
				.build();
		return ContainerItem.newListBuilder()
				.withContainer(large, 1, cost(100, 2))
				.withContainer(small, 2, cost(40, 1))
				.build();
	}

	private BoxItem twoBoxes() {
		return new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build(), 2);
	}

	private FixedContainerCostCalculator cost(long cost) {
		return cost(cost, 1_000);
	}

	private FixedContainerCostCalculator cost(long cost, long volume) {
		return new FixedContainerCostCalculator(cost, volume, null, 0);
	}

	private Container container(String id) {
		return container(id, 10);
	}

	private Container container(String id, int size) {
		return Container.newBuilder()
				.withId(id)
				.withSize(size, size, size)
				.withMaxLoadWeight(100)
				.build();
	}

	private BoxItem boxItem() {
		return new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build(), 1);
	}

}
