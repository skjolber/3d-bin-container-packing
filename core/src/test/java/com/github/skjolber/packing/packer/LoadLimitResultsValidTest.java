package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;

/**
 * Packing results with load limits must satisfy the load validators: the controls which
 * decide placements and the validators must agree on weight, pressure, box count and
 * identical-box limits.
 */
public class LoadLimitResultsValidTest {

	private static final int SEEDS = 40;

	@Test
	public void plainPackager() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check(packager);
		}
	}

	@Test
	public void largestAreaFitFirstPackager() {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			check(packager);
		}
	}

	@Test
	public void fastLargestAreaFitFirstPackager() {
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build()) {
			check(packager);
		}
	}

	/**
	 * Brute force reuses its placements between searches, and results link their loads when building their stack: a
	 * search must not keep those links (which made it fail, or check loads against stale weights).
	 */
	@Test
	public void bruteForcePackagers() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> BruteForcePackager.newBuilder().build(),
				() -> FastBruteForcePackager.newBuilder().build(),
				() -> ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		for(Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				List<String> failures = new ArrayList<>();
				for(long seed = 0; seed < SEEDS; seed++) {
					// few boxes: brute force is exponential
					Random random = new Random(seed);
					List<BoxItem> items = new ArrayList<>();
					for(int i = 0; i < 5; i++) {
						Box box = Box.newBuilder().withId("b" + i).withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2)).withRotate2D()
								.withWeight(1 + random.nextInt(3)).withMaxLoadWeight(1 + random.nextInt(4)).build();
						items.add(new BoxItem(box, 1));
					}
					Container container = Container.newBuilder().withId("c").withSize(4, 3, 3).withMaxLoadWeight(100).build();
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(List.of(new ContainerItem(container, 6)))
							.withBoxItems(items)
							.withMaxContainerCount(6)
							.withInterruptDuration(10_000)
							.build();
					if(!result.isSuccess()) {
						failures.add("seed " + seed + ": not packed");
					}
					for(int c = 0; c < result.size(); c++) {
						List<ValidatorResultReason> reasons = new ArrayList<>();
						LoadValidator validator = new DefaultLoadValidatorBuilder()
								.withPlacements(result.get(c).getStack().getPlacements())
								.withContainer(result.get(c))
								.build();
						if(!validator.isValid(result.get(c).getStack().getPlacements(), reasons)) {
							failures.add("seed " + seed + " container " + c + ": " + reasons);
						}
					}
				}
				assertThat(failures).as(packager.getClass().getSimpleName()).isEmpty();
			}
		}
	}

	private static void check(AbstractPackager<?> packager) {
		List<String> failures = new ArrayList<>();
		int validated = 0;
		for(int load : new int[] { PackagerGoldenMasterTest.LOAD_WEIGHT, PackagerGoldenMasterTest.LOAD_WEIGHT_PRESSURE_COUNT, PackagerGoldenMasterTest.LOAD_IDENTICAL }) {
			for(long seed = 0; seed < SEEDS; seed++) {
				Random random = new Random(seed);
				List<BoxItem> items = PackagerGoldenMasterTest.createItems(random, new Random(seed * 31 + 7), 40, 2, load);
				List<ContainerItem> containers = PackagerGoldenMasterTest.createContainers(random);

				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers)
						.withBoxItems(items)
						.withMaxContainerCount(4)
						.build();

				for(int c = 0; c < result.size(); c++) {
					Container container = result.get(c);
					LoadValidator validator = new DefaultLoadValidatorBuilder()
							.withPlacements(container.getStack().getPlacements())
							.withContainer(container)
							.build();
					if(validator == null) {
						continue;
					}
					validated++;
					List<ValidatorResultReason> reasons = new ArrayList<>();
					if(!validator.isValid(container.getStack().getPlacements(), reasons)) {
						failures.add("load " + load + " seed " + seed + " container " + c + ": " + reasons);
					}
				}
			}
		}
		assertThat(validated).isGreaterThan(SEEDS);
		assertThat(failures).isEmpty();
	}
}
