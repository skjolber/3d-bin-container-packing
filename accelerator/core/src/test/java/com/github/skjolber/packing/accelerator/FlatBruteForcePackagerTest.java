package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.accelerator.FlatTestInstances.Instance;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;

/**
 * The brute force packager (every box at every free point where it fits) with the CPU engine as its accelerator, searching with all points: the
 * placements of the packager without an accelerator in exact mode, its values when any best arrangement will do.
 */
class FlatBruteForcePackagerTest {

	private static final long INTERRUPT_MILLIS = 60_000L;

	private static String describe(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (Container container : result.getContainers()) {
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getBoxItem().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',')
						.append(placement.getAbsoluteZ()).append('+').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append('x')
						.append(placement.getStackValue().getDz()).append(' ');
			}
			builder.append("| ");
		}
		return builder.toString();
	}

	private static String values(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (Container container : result.getContainers()) {
			builder.append(container.getStack().getVolume()).append('/').append(container.getStack().getWeight()).append('/').append(container.getStack().getPlacements().size()).append(' ');
		}
		return builder.toString();
	}

	private static PackagerResult pack(BruteForcePackager packager, Instance instance) {
		List<ContainerItem> containerItems = ContainerItem.newListBuilder().withContainer(instance.container, 8).build();
		List<BoxItem> boxItems = instance.copyBoxItems();
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	@Test
	void samePlacementsAsThePackagerWithoutAccelerator() {
		FlatEngine engine = FlatEngine.newBuilder().withThreads(2).withMinBoxes(1).withTree(true).build();
		BruteForcePackager unaccelerated = BruteForcePackager.newBuilder().withTreeSearchAnyBest(false).build();
		AcceleratedBruteForcePackager accelerated = AcceleratedBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		AcceleratedBruteForcePackager raced = AcceleratedBruteForcePackager.newBuilder().withTreeSearchAnyBest(false)
				.withAccelerator(engine).withAcceleratorMode(AcceleratorMode.AUTO).withAcceleratorGate(0).build();
		try {
			for (int seed = 0; seed < 60; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 8);
				String expected = describe(pack(unaccelerated, instance));
				assertThat(expected).as("seed " + seed).isNotBlank();
				assertThat(describe(pack(accelerated, instance))).as("seed " + seed).isEqualTo(expected);
				assertThat(describe(pack(raced, instance))).as("seed " + seed + " raced").isEqualTo(expected);
			}
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
			assertThat(accelerated.getAcceleratedCount()).isGreaterThan(60);
			assertThat(raced.getAcceleratorMismatchCount()).isZero();
		} finally {
			unaccelerated.close();
			accelerated.close();
			raced.close();
			engine.close();
		}
	}

	@Test
	void sameValuesWhenAnyBestWillDo() {
		FlatEngine engine = FlatEngine.newBuilder().withThreads(2).withMinBoxes(1).withTree(true).build();
		BruteForcePackager unaccelerated = BruteForcePackager.newBuilder().build();
		AcceleratedBruteForcePackager accelerated = AcceleratedBruteForcePackager.newBuilder().withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			for (int seed = 0; seed < 60; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 8);
				assertThat(values(pack(accelerated, instance))).as("seed " + seed).isEqualTo(values(pack(unaccelerated, instance)));
			}
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
			assertThat(accelerated.getAcceleratedCount()).isGreaterThan(60);
		} finally {
			unaccelerated.close();
			accelerated.close();
			engine.close();
		}
	}

	@Test
	void anEngineWithoutTheTreeSearchDoesNotApply() {
		// the loop over the orders takes the best point of each rotation, not all points: the packager searches itself
		FlatEngine engine = FlatEngine.newBuilder().withThreads(2).withMinBoxes(1).withTree(false).build();
		BruteForcePackager unaccelerated = BruteForcePackager.newBuilder().withTreeSearchAnyBest(false).build();
		AcceleratedBruteForcePackager accelerated = AcceleratedBruteForcePackager.newBuilder().withTreeSearchAnyBest(false).withAccelerator(engine).withAcceleratorMode(AcceleratorMode.GPU_ONLY).build();
		try {
			for (int seed = 0; seed < 10; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 8);
				assertThat(describe(pack(accelerated, instance))).as("seed " + seed).isEqualTo(describe(pack(unaccelerated, instance)));
			}
			assertThat(accelerated.getAcceleratedCount()).isZero();
			assertThat(accelerated.getAcceleratorFallbackCount()).isZero();
			assertThat(accelerated.getAcceleratorMismatchCount()).isZero();
		} finally {
			unaccelerated.close();
			accelerated.close();
			engine.close();
		}
	}
}
