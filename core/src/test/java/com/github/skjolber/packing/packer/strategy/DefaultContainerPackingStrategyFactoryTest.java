package com.github.skjolber.packing.packer.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategy;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.strategy.cost.LowestCostContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.ordered.OrderedContainerPackingStrategy;

/**
 * The container packing strategy factory gets what the strategies need from the packager: its result comparator and its empty
 * result. The default factory is stateless, and creates a new strategy for every packaging operation.
 */
public class DefaultContainerPackingStrategyFactoryTest {

	private static final long INTERRUPT_DURATION = 60_000L;

	/** A factory which records its arguments, and delegates to the default factory. */
	private static class RecordingFactory implements ContainerPackingStrategyFactory {

		private final ContainerPackingStrategyFactory delegate = new DefaultContainerPackingStrategyFactory();

		private final List<IntermediatePackagerResultComparator> comparators = new ArrayList<>();
		private final List<IntermediatePackagerResult> emptyResults = new ArrayList<>();
		private final List<ContainerPackingStrategy> strategies = new ArrayList<>();

		@Override
		public synchronized ContainerPackingStrategy create(ContainerInventory inventory, List<BoxItem> boxItems,
				List<BoxItemGroup> boxItemGroups, IntermediatePackagerResultComparator comparator,
				Supplier<IntermediatePackagerResult> emptyResultSupplier) {
			comparators.add(comparator);
			emptyResults.add(emptyResultSupplier.get());

			ContainerPackingStrategy strategy = delegate.create(inventory, boxItems, boxItemGroups, comparator, emptyResultSupplier);
			strategies.add(strategy);
			return strategy;
		}
	}

	private static List<BoxItem> boxItems() {
		return List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1));
	}

	private static List<ContainerItem> containers() {
		return List.of(new ContainerItem(Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 1));
	}

	private static PackagerResult pack(AbstractPackager<?> packager, List<ContainerItem> containers) {
		return packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItems(boxItems())
				.withInterruptDuration(INTERRUPT_DURATION)
				.build();
	}

	private static void assertReceivesThePackagersArguments(AbstractPackager<?> packager, RecordingFactory factory, IntermediatePackagerResultComparator comparator) {
		try (packager) {
			assertThat(pack(packager, containers()).isSuccess()).isTrue();
			assertThat(pack(packager, containers()).isSuccess()).isTrue();

			assertThat(factory.comparators).as(packager.getClass().getSimpleName()).isNotEmpty();
			// the packager's own comparator, not one which the factory makes up
			assertThat(factory.comparators).as(packager.getClass().getSimpleName()).allSatisfy(c -> assertThat(c).isSameAs(comparator));
			assertThat(factory.emptyResults).as(packager.getClass().getSimpleName()).allSatisfy(r -> assertThat(r.isEmpty()).isTrue());

			// a strategy for each packaging operation
			assertThat(factory.strategies).as(packager.getClass().getSimpleName()).doesNotHaveDuplicates();
		}
	}

	@Test
	public void packagersPassTheirComparatorAndEmptyResult() {
		IntermediatePackagerResultComparator comparator = new BruteForceIntermediatePackagerResultComparator();

		RecordingFactory factory = new RecordingFactory();
		assertReceivesThePackagersArguments(PlainPackager.newBuilder()
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(LargestAreaFitFirstPackager.newBuilder()
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(FastLargestAreaFitFirstPackager.newBuilder()
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(BruteForcePackager.newBuilder()
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(FastBruteForcePackager.newBuilder()
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(ParallelBruteForcePackager.newBuilder().withThreads(2)
				.withIntermediatePackagerResultComparator(comparator).withContainerPackingStrategyFactory(factory).build(), factory, comparator);

		factory = new RecordingFactory();
		assertReceivesThePackagersArguments(CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withIntermediatePackagerResultComparator(comparator)
				.withContainerPackingStrategyFactory(factory).build(), factory, comparator);
	}

	@Test
	public void defaultFactoryCreatesAStrategyForEachCall() {
		RecordingFactory factory = new RecordingFactory();
		try (PlainPackager packager = PlainPackager.newBuilder().withContainerPackingStrategyFactory(factory).build()) {
			pack(packager, containers());
			pack(packager, containers());
		}
		assertThat(factory.strategies).hasSize(2);
		assertThat(factory.strategies.get(0)).isInstanceOf(OrderedContainerPackingStrategy.class);
		assertThat(factory.strategies.get(1)).isInstanceOf(OrderedContainerPackingStrategy.class);
		assertThat(factory.strategies.get(0)).isNotSameAs(factory.strategies.get(1));
	}

	@Test
	public void defaultFactorySelectsCostAwarePackingForContainerCosts() {
		List<ContainerItem> costed = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("costed").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 1, new FixedContainerCostCalculator(5, 1_000, null, 0))
				.build();

		RecordingFactory factory = new RecordingFactory();
		try (PlainPackager packager = PlainPackager.newBuilder().withContainerPackingStrategyFactory(factory).build()) {
			assertThat(pack(packager, costed).isSuccess()).isTrue();
			assertThat(pack(packager, costed).isSuccess()).isTrue();
		}
		assertThat(factory.strategies).hasSize(2);
		assertThat(factory.strategies).allSatisfy(strategy -> assertThat(strategy).isInstanceOf(LowestCostContainerPackingStrategy.class));
		assertThat(factory.strategies.get(0)).isNotSameAs(factory.strategies.get(1));
	}
}
