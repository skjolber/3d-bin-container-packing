package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Unloading;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;

/**
 * Packing results with load limits must satisfy the load validators: the controls which
 * decide placements and the validators must agree on weight, pressure, box count and
 * identical-box limits, for each way of unloading.
 */
public class LoadLimitResultsValidTest {

	private static final int SEEDS = 40;

	@ParameterizedTest
	@EnumSource(Unloading.class)
	public void plainPackager(Unloading unloading) {
		try (PlainPackager packager = PlainPackager.newBuilder().withUnloading(unloading).build()) {
			check(packager, unloading);
		}
	}

	@ParameterizedTest
	@EnumSource(Unloading.class)
	public void largestAreaFitFirstPackager(Unloading unloading) {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withUnloading(unloading).build()) {
			check(packager, unloading);
		}
	}

	@ParameterizedTest
	@EnumSource(Unloading.class)
	public void fastLargestAreaFitFirstPackager(Unloading unloading) {
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withUnloading(unloading).build()) {
			check(packager, unloading);
		}
	}

	private static void check(AbstractPackager<?> packager, Unloading unloading) {
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
							.withUnloading(unloading)
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
