package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastPointCalculator3DStack;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.accelerator.FlatTestInstances.Instance;

/**
 * With an accelerator the packager must return exactly the placements of the packager without one: the accelerator only finds the permutation, which the
 * packager then places itself.
 */
public abstract class AbstractFlatAcceleratedPackagerTest {

	/** @return the engine under test, with the options of a {@linkplain FlatEngine} */
	protected abstract BruteForceAccelerator engine(FlatEngineOptions options);


	private static final long INTERRUPT_MILLIS = 60_000L;

	private static String describe(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (Container container : result.getContainers()) {
			builder.append(container.getId()).append('[');
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getBoxItem().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',')
						.append(placement.getAbsoluteZ()).append('+').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append('x')
						.append(placement.getStackValue().getDz()).append(' ');
			}
			builder.append("] ");
		}
		return builder.toString();
	}

	private static PackagerResult pack(FastBruteForcePackager packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	private static List<ContainerItem> containerItems(Instance instance, long seed) {
		// every box fits a container alone: enough containers for all boxes
		ContainerItem.Builder builder = ContainerItem.newListBuilder().withContainer(instance.container, 8);
		if(seed % 2 == 0) {
			// a smaller second container: the result of the first is a bound for the second
			Container second = Container.newBuilder().withId("second" + seed)
					.withEmptyWeight(1)
					.withSize(Math.max(3, instance.container.getLoadDx() - 1), instance.container.getLoadDy(), Math.max(3, instance.container.getLoadDz() - 1))
					.withMaxLoadWeight(100_000)
					.build();
			builder.withContainer(second, 8);
		}
		return builder.build();
	}

	private BruteForceAccelerator engine(int threads) {
		return engine(new FlatEngineOptions().withThreads(threads).withMinBoxes(1));
	}

	/**
	 * Pack random instances with and without an accelerator.
	 *
	 * @return the accelerated packager, which has the counters
	 */
	private AcceleratedFastBruteForcePackager comparePacking(BruteForceAccelerator engine, int fromSeed, int toSeed, int minBoxes, int maxBoxes, UnaryOperator<FastBruteForcePackager.Builder> configure,
			Function<Instance, List<BoxItem>> boxItems) {
		// the accelerator picks the loop's winner, so the packager without it must too: exact tree search, not any arrangement of the best value
		FastBruteForcePackager unaccelerated = configure.apply(FastBruteForcePackager.newBuilder()).withTreeSearchAnyBest(false).build();
		AcceleratedFastBruteForcePackager.Builder builder = AcceleratedFastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY);
		configure.apply(builder);
		AcceleratedFastBruteForcePackager accelerated = builder.build();
		try {
			for (int seed = fromSeed; seed < toSeed; seed++) {
				Instance instance = FlatTestInstances.random(seed, minBoxes, maxBoxes);
				List<ContainerItem> containerItems = containerItems(instance, seed);
				String expected = describe(pack(unaccelerated, containerItems, boxItems.apply(instance)));
				assertThat(expected).as("seed " + seed + " packs all boxes").isNotEmpty();
				assertThat(describe(pack(accelerated, containerItems, boxItems.apply(instance)))).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			unaccelerated.close();
			accelerated.close();
		}
		return accelerated;
	}

	@Test
	void samePlacementsAsThePackagerWithoutAccelerator() {
		BruteForceAccelerator engine = engine(4);
		try {
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 80, 5, 8, b -> b, Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isGreaterThan(80);
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void samePlacementsWhenTheEngineRunsOnTheCallingThread() {
		BruteForceAccelerator engine = engine(1);
		try {
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 100, 130, 6, 8, b -> b, Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isGreaterThan(30);
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void fallsBackWhenTheEngineGivesUp() {
		BruteForceAccelerator engine = engine(new FlatEngineOptions().withThreads(2).withMinBoxes(1).withMaxPoints(1));
		try {
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 20, 6, 8, b -> b, Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratorFallbackCount()).isGreaterThan(0);
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void leavesProblemsTheEngineDoesNotSupportToThePackager() {
		BruteForceAccelerator engine = engine(new FlatEngineOptions().withThreads(2).withMinBoxes(20));
		try {
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 10, 5, 8, b -> b, Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateWithRequireFullSupport() {
		BruteForceAccelerator engine = engine(2);
		try {
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 10, 5, 8, b -> b.withRequireFullSupport(true), Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateWithAnotherPointComparator() {
		BruteForceAccelerator engine = engine(2);
		try {
			FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator comparator = (stackValue, bestPoint, candidatePoint) -> candidatePoint.getMinZ() < bestPoint.getMinZ() ? 1 : -1;
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 10, 5, 8, b -> b.withPointComparator(comparator), Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateWithAnotherResultComparator() {
		BruteForceAccelerator engine = engine(2);
		try {
			// does not prefer load volume
			AcceleratedFastBruteForcePackager accelerated = comparePacking(engine, 0, 10, 5, 8,
					b -> b.withIntermediatePackagerResultComparator((a, other) -> Integer.compare(a.getStack().size(), other.getStack().size())), Instance::copyBoxItems);
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			engine.close();
		}
	}

	@Test
	void samePlacementsWithExtractionOrders() {
		BruteForceAccelerator engine = engine(2);
		try {
			comparePacking(engine, 0, 20, 5, 8, b -> b, instance -> {
				List<BoxItem> result = new ArrayList<>();
				int order = 0;
				for (BoxItem boxItem : instance.copyBoxItems()) {
					result.add(boxItem.withExtractionOrder(order++ % 2));
				}
				return result;
			});
		} finally {
			engine.close();
		}
	}

	@Test
	void samePlacementsWithContainerPriorities() {
		BruteForceAccelerator engine = engine(2);
		try {
			comparePacking(engine, 0, 20, 5, 8, b -> b, instance -> {
				List<BoxItem> result = new ArrayList<>();
				int priority = 0;
				for (BoxItem boxItem : instance.copyBoxItems()) {
					result.add(boxItem.withContainerPriority(priority++ % 2));
				}
				return result;
			});
		} finally {
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateWithObstacles() {
		BruteForceAccelerator engine = engine(2);
		FastBruteForcePackager unaccelerated = FastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).build();
		AcceleratedFastBruteForcePackager accelerated = AcceleratedFastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			for (int seed = 0; seed < 10; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 8);
				Container container = instance.container;
				String[] results = new String[2];
				FastBruteForcePackager[] packagers = { unaccelerated, accelerated };
				for (int p = 0; p < packagers.length; p++) {
					PackagerResult result = packagers[p].newResultBuilder().withContainerItem(b -> {
						b.withContainerItem(new ContainerItem(container, 8));
						b.withObstacles(o -> o.withObstacle(0, 0, 0, 1, 1, 1));
					}).withBoxItems(instance.copyBoxItems()).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
					results[p] = describe(result);
				}
				assertThat(results[1]).as("seed " + seed).isEqualTo(results[0]);
			}
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			unaccelerated.close();
			accelerated.close();
			engine.close();
		}
	}

	private static BoxItemPermutationRotationIterator iterator(Container container, int extractionOrderOfSecond, int priorityOfSecond) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			BoxItem boxItem = new BoxItem(Box.newBuilder().withId("b" + i).withSize(2, 2, 1 + i).withWeight(1).withRotate3D().build(), 2);
			if(i == 1) {
				boxItem.withExtractionOrder(extractionOrderOfSecond).withContainerPriority(priorityOfSecond);
			}
			boxItems.add(boxItem);
		}
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(boxItems)
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
	}

	/** the hook of the session, for an iterator: the search of one container, with the accelerator where it applies */
	private static Object packContainer(AcceleratedFastBruteForcePackager packager, Container container, BoxItemPermutationRotationIterator iterator, boolean interrupt)
			throws PackagerInterruptedException {
		ContainerItem containerItem = ContainerItem.newListBuilder().withContainer(container).build().get(0);
		int n = iterator.length();
		return packager.packContainer(new FastPointCalculator3DStack(n + 1), FlatSearchOracle.placements(n), 0, containerItem, 0, iterator, () -> interrupt, null, n);
	}

	private static Container container() {
		return Container.newBuilder().withId("c").withEmptyWeight(1).withSize(4, 4, 4).withMaxLoadWeight(100).build();
	}

	@Test
	void acceleratesIteratorsWithTheSameExtractionOrderAndPriority() throws PackagerInterruptedException {
		BruteForceAccelerator engine = engine(2);
		AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			assertThat(packContainer(packager, container(), iterator(container(), 0, 0), false)).isNotNull();
			assertThat(packager.getAcceleratedCount()).isEqualTo(1);
			assertThat(packager.getAcceleratorMismatchCount()).isZero();
		} finally {
			packager.close();
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateIteratorsWithDifferentExtractionOrders() throws PackagerInterruptedException {
		BruteForceAccelerator engine = engine(2);
		AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			assertThat(packContainer(packager, container(), iterator(container(), 1, 0), false)).isNotNull();
			assertThat(packager.getAcceleratedCount()).isZero();
			assertThat(packager.getAcceleratorFallbackCount()).isZero();
		} finally {
			packager.close();
			engine.close();
		}
	}

	@Test
	void doesNotAccelerateIteratorsWithDifferentContainerPriorities() throws PackagerInterruptedException {
		BruteForceAccelerator engine = engine(2);
		AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			assertThat(packContainer(packager, container(), iterator(container(), 0, 1), false)).isNotNull();
			assertThat(packager.getAcceleratedCount()).isZero();
			assertThat(packager.getAcceleratorFallbackCount()).isZero();
		} finally {
			packager.close();
			engine.close();
		}
	}

	@Test
	void interruptIsHonouredWhenAccelerated() {
		BruteForceAccelerator engine = engine(2);
		AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			assertThatThrownBy(() -> packContainer(packager, container(), iterator(container(), 0, 0), true)).isInstanceOf(PackagerInterruptedException.class);
		} finally {
			packager.close();
			engine.close();
		}
	}

	@Test
	void cpuOnlyNeverUsesTheAccelerator() {
		BruteForceAccelerator engine = engine(2);
		FastBruteForcePackager unaccelerated = FastBruteForcePackager.newBuilder().build();
		AcceleratedFastBruteForcePackager accelerated = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.CPU_ONLY).build();
		try {
			for (int seed = 0; seed < 20; seed++) {
				Instance instance = FlatTestInstances.random(seed, 8, 10);
				List<ContainerItem> containerItems = containerItems(instance, seed);
				assertThat(describe(pack(accelerated, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(describe(pack(unaccelerated, containerItems, instance.copyBoxItems())));
			}
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorRaceCpuCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
		} finally {
			unaccelerated.close();
			accelerated.close();
			engine.close();
		}
	}

	/** In auto mode the packager and the accelerator search alongside, each stopped by the other's result: the placements are the exact packager's either way. */
	@Test
	void autoRacesTheAcceleratorAndAgrees() {
		BruteForceAccelerator engine = engine(2);
		FastBruteForcePackager unaccelerated = FastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).build();
		// no gate: the accelerator joins at once, so that it wins some containers; a gate of a second: the packager finishes every container first
		AcceleratedFastBruteForcePackager raced = AcceleratedFastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.AUTO).withAcceleratorGate(0).build();
		AcceleratedFastBruteForcePackager gated = AcceleratedFastBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.AUTO).withAcceleratorGate(1000).build();
		try {
			for (int seed = 0; seed < 40; seed++) {
				Instance instance = FlatTestInstances.random(seed, 8, 11);
				List<ContainerItem> containerItems = containerItems(instance, seed);
				String expected = describe(pack(unaccelerated, containerItems, instance.copyBoxItems()));
				assertThat(describe(pack(raced, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(expected);
				assertThat(describe(pack(gated, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(expected);
			}
			assertThat(raced.getAcceleratorMismatchCount()).isZero();
			assertThat(raced.getAcceleratedCount() + raced.getAcceleratorRaceCpuCount()).isGreaterThan(40);
			// with the gate the packager finishes nearly every container first; a slow machine may let the accelerator win a hard one
			assertThat(gated.getAcceleratorMismatchCount()).isZero();
			assertThat(gated.getAcceleratedCount() + gated.getAcceleratorRaceCpuCount()).isGreaterThan(40);
			assertThat(gated.getAcceleratorRaceCpuCount()).isGreaterThan(gated.getAcceleratedCount());
		} finally {
			unaccelerated.close();
			raced.close();
			gated.close();
			engine.close();
		}
	}

	/**
	 * When any best arrangement will do, with an accelerator which does not search a state twice ({@link FlatStateCache}): each container has the
	 * volume, the weight and the number of boxes of the packager without an accelerator, and every winner is confirmed by the packager.
	 */
	@Test
	void sameValuesWithTheStateCacheWhenAnyBestWillDo() {
		BruteForceAccelerator engine = engine(new FlatEngineOptions().withThreads(2).withMinBoxes(1).withTree(true).withStateCache(1024));
		FastBruteForcePackager unaccelerated = FastBruteForcePackager.newBuilder().build();
		AcceleratedFastBruteForcePackager accelerated = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			for (int seed = 0; seed < 80; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 9);
				List<ContainerItem> containerItems = containerItems(instance, seed);
				String[] values = new String[2];
				FastBruteForcePackager[] packagers = { unaccelerated, accelerated };
				for (int p = 0; p < 2; p++) {
					StringBuilder builder = new StringBuilder();
					for (Container container : pack(packagers[p], containerItems, instance.copyBoxItems()).getContainers()) {
						builder.append(container.getStack().getVolume()).append('/').append(container.getStack().getWeight()).append('/').append(container.getStack().getPlacements().size()).append(' ');
					}
					values[p] = builder.toString();
				}
				assertThat(values[1]).as("seed " + seed).isEqualTo(values[0]);
			}
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
			assertThat(accelerated.getAcceleratedCount()).isGreaterThan(80);
		} finally {
			unaccelerated.close();
			accelerated.close();
			engine.close();
		}
	}
}
