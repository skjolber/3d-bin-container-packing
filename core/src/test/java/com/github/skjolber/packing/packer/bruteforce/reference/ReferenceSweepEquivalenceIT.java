package com.github.skjolber.packing.packer.bruteforce.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.reference.ReferenceComparison.Quality;

/**
 * Slow integration test (run with the {@code slow-tests} profile): the 5.0 brute force search against the 4.x
 * one (see {@link Version4Reference}), over thousands of seeded scenarios. Larger than the fast {@link SearchEquivalenceTest}
 * by two orders of magnitude, for the cases which the few hand-picked and seeded instances there do not reach.
 *
 * <pre>
 *   seeded random scenarios -------+
 *                                  +--> reference recursive search ----------------------> quality
 *   guillotine scenarios           |                                                          ==
 *   (a perfect packing exists) ----+--> 5.0 brute force, one container ------------------> quality
 *                                  +--> 5.0 brute force, skipping reverse permutations --> quality
 *                                       == reference which skips the same permutations
 * </pre>
 *
 * All searches have an interrupt deadline, so that a regression cannot hang the build: it fails the test instead.
 */
class ReferenceSweepEquivalenceIT {

	/** Interrupt of each search; most searches take milliseconds, the largest of the sweep some seconds. */
	private static final long INTERRUPT_MILLIS = 120_000;

	private static final int RANDOM_SCENARIOS = 5000;

	private static final int GUILLOTINE_SCENARIOS = 2000;

	private static final int MAX_BOXES = 8;

	private static final int GUILLOTINE_MAX_BOXES = 14;

	private static final long GUILLOTINE_MAX_STATES = 2_000_000;

	private static final long MAX_STATES = 1_000_000;

	/**
	 * Random instances with a fixed seed, with 2-8 boxes: mixed rotations, duplicates, boxes which do not fit and a load
	 * weight limit, in containers which are mostly filled or mostly empty.
	 */
	@Test
	void randomScenarios() throws PackagerInterruptedException {
		int all = 0;
		int partial = 0;
		int none = 0;
		int oversized = 0;
		int rotated = 0;
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build();
				BruteForcePackager skippingPackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			for (int seed = 0; seed < RANDOM_SCENARIOS; seed++) {
				ReferenceScenario scenario = ReferenceScenario.random(seed, MAX_BOXES, MAX_STATES, seed % 2 == 0);

				Quality quality = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

				if(quality.boxCount() == scenario.boxCount()) {
					all++;
				} else if(quality.boxCount() > 0) {
					partial++;
				} else {
					none++;
				}
				com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator iterator = ReferenceComparison.newReferenceIterator(scenario);
				if(!iterator.getExcluded().isEmpty()) {
					oversized++;
				}
				if(iterator.length() > 0 && iterator.countRotations() > 1) {
					rotated++;
				}
			}
		}
		// the sweep includes searches which place everything, searches for the best subset and boxes which never fit
		assertThat(all).as("instances where all boxes fit").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 5);
		assertThat(partial).as("instances where only some boxes fit").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 5);
		assertThat(none).as("instances where no box fits").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 20);
		assertThat(oversized).as("instances with boxes which do not fit on their own").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 5);
		assertThat(rotated).as("instances with rotations").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 4);
	}

	/**
	 * Instances which are split recursively into boxes that fill the container exactly: a packing of all the boxes exists,
	 * and both the reference and 5.0 must find one (with or without skipping reverse permutations).
	 */
	@Test
	void guillotineScenarios() throws PackagerInterruptedException {
		int duplicates = 0;
		int rotated = 0;
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build();
				BruteForcePackager skippingPackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			for (int seed = 0; seed < GUILLOTINE_SCENARIOS; seed++) {
				ReferenceScenario scenario = ReferenceScenario.guillotine(seed, GUILLOTINE_MAX_BOXES, GUILLOTINE_MAX_STATES);

				Quality quality = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

				// the boxes fill the container
				assertThat(quality).as("%s", scenario).isEqualTo(new Quality(scenario.containerVolume(), scenario.boxCount(), scenario.boxCount()));

				assertPublicApiPlacesAll(packager, scenario);
				assertPublicApiPlacesAll(skippingPackager, scenario);

				if(scenario.specs().size() < scenario.boxCount()) {
					duplicates++;
				}
				com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator iterator = ReferenceComparison.newReferenceIterator(scenario);
				if(iterator.countRotations() > 1) {
					rotated++;
				}
			}
		}
		assertThat(duplicates).as("instances with duplicate boxes").isGreaterThanOrEqualTo(GUILLOTINE_SCENARIOS / 4);
		assertThat(rotated).as("instances with rotations").isGreaterThanOrEqualTo(GUILLOTINE_SCENARIOS / 4);
	}

	/**
	 * The packager through its public API, for one container: it must place all boxes, filling the container.
	 */
	private static void assertPublicApiPlacesAll(BruteForcePackager packager, ReferenceScenario scenario) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(scenario.newContainer(), 1)))
				.withBoxItems(scenario.newBoxItems())
				.withMaxContainerCount(1)
				.withInterruptDuration(INTERRUPT_MILLIS)
				.build();

		assertThat(result.isTimeout()).as("%s", scenario).isFalse();
		assertThat(result.isSuccess()).as("%s", scenario).isTrue();
		assertThat(result.size()).as("%s", scenario).isEqualTo(1);
		Stack stack = result.get(0).getStack();
		assertThat(stack.size()).as("%s", scenario).isEqualTo(scenario.boxCount());
		assertThat(stack.getVolume()).as("%s", scenario).isEqualTo(scenario.containerVolume());
	}

}
