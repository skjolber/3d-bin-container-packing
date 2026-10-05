package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.InsertionOrderValidator;

/**
 * Packing results are in a possible insertion order, for each container access: without a box item order, the
 * placements are sequenced after packing; with an order, only insertable boxes are placed.
 */
public class InsertionOrderResultsTest {

	private static final int SEEDS = 20;

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void orderedResultsAreInAPossibleInsertionOrder(ContainerAccess access) {
		// with a box item order, the packagers only place boxes which can be inserted after the boxes already there
		for (Order order : new Order[] { Order.CHRONOLOGICAL, Order.CHRONOLOGICAL_ALLOW_SKIPPING }) {
			check(access, order, List.of(
					() -> PlainPackager.newBuilder().build(),
					() -> LargestAreaFitFirstPackager.newBuilder().build(),
					() -> FastLargestAreaFitFirstPackager.newBuilder().build()));
		}
	}

	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void resultsAreInAPossibleInsertionOrder(ContainerAccess access) {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().build(),
				() -> LargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> CompositePackager.newBuilder().withPackager(PlainPackager.newBuilder().build()).withPackager(FastBruteForcePackager.newBuilder().build(), 1000).build());
		check(access, Order.NONE, packagers);
	}

	private static void check(ContainerAccess access, Order order, List<Supplier<AbstractPackager<?>>> packagers) {
		InsertionOrderValidator validator = new InsertionOrderValidator();
		List<String> failures = new ArrayList<>();
		int containers = 0;
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (long seed = 0; seed < SEEDS; seed++) {
					Random random = new Random(seed);
					List<BoxItem> items = new ArrayList<>();
					for (int i = 0; i < 6; i++) {
						items.add(new BoxItem(Box.newBuilder().withId("b" + i).withSize(2 + random.nextInt(8), 2 + random.nextInt(8), 1 + random.nextInt(6)).withRotate3D().withWeight(1).build(), 1 + random.nextInt(3)));
					}
					List<ContainerItem> containerItems = ContainerItem.newListBuilder()
							.withContainer(Container.newBuilder().withId("c").withSize(20, 15, 12).withMaxLoadWeight(100_000).withAccess(access).build(), 4)
							.build();
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(containerItems)
							.withBoxItems(items)
							.withMaxContainerCount(4)
							.withInterruptDuration(5_000)
							.withOrder(order)
							.build();
					for (Container container : result.getContainers()) {
						containers++;
						assertThat(container.getAccess()).isEqualTo(access);
						List<ValidatorResultReason> reasons = new ArrayList<>();
						if(!validator.validate(container.getStack().getPlacements(), access, reasons)) {
							failures.add(packager.getClass().getSimpleName() + " " + order + " seed " + seed + ": " + reasons.get(0).getMessage());
						}
					}
				}
			}
		}
		assertThat(containers).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}

	/**
	 * Without insertion order, the placements are in the order of the packager's search, which is often not a possible
	 * insertion order; the insertion order can be calculated later.
	 */
	@ParameterizedTest
	@EnumSource(ContainerAccess.class)
	public void insertionOrderCanBeSkippedAndCalculatedLater(ContainerAccess access) {
		InsertionOrderValidator validator = new InsertionOrderValidator();
		int notInInsertionOrder = 0;
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Random random = new Random(seed);
				List<BoxItem> items = new ArrayList<>();
				for (int i = 0; i < 15; i++) {
					items.add(new BoxItem(Box.newBuilder().withId("b" + i).withSize(2 + random.nextInt(8), 2 + random.nextInt(8), 1 + random.nextInt(6)).withRotate3D().withWeight(1).build(), 1 + random.nextInt(3)));
				}
				List<ContainerItem> containerItems = ContainerItem.newListBuilder()
						.withContainer(Container.newBuilder().withId("c").withSize(20, 15, 12).withMaxLoadWeight(100_000).withAccess(access).build(), 4)
						.build();
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containerItems)
						.withBoxItems(items)
						.withMaxContainerCount(4)
						.withInsertionOrder(false)
						.build();
				for (Container container : result.getContainers()) {
					if(!validator.validate(container.getStack().getPlacements(), access, new ArrayList<>())) {
						notInInsertionOrder++;
					}
				}
				// later
				assertThat(InsertionSequencer.sequence(result.getContainers(), Order.NONE)).isTrue();
				for (Container container : result.getContainers()) {
					assertThat(validator.validate(container.getStack().getPlacements(), access, new ArrayList<>())).isTrue();
				}
			}
		}
		assertThat(notInInsertionOrder).isGreaterThan(0);
	}
}
