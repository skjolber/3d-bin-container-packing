package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
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
