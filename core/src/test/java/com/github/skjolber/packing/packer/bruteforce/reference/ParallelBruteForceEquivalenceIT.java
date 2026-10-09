package com.github.skjolber.packing.packer.bruteforce.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.reference.ReferenceComparison.Quality;

/**
 * Slow integration test (run with the {@code slow-tests} profile): the parallel 5.0 brute force search against the
 * 4.x one (see {@link Version4Reference}), on two and four threads, with and without skipping reverse permutations.
 * The parallel search splits the permutations between its threads, so it has to select the best of the results of the
 * threads and, when skipping, start each thread at its first canonical permutation: it must still find a best packing
 * as good as the reference's.
 * <p>
 * The corpus is smaller than that of {@link ReferenceSweepEquivalenceIT}, as each search starts work units on the
 * threads. A scenario in which no box fits the container is checked through the public API, because the session of
 * the parallel packager (unlike that of {@code BruteForcePackager}) throws {@code ArrayIndexOutOfBoundsException}
 * from {@code attempt(..)} when there is nothing to permute.
 * <p>
 * All searches have an interrupt deadline, so that a regression cannot hang the build: it fails the test instead.
 */
class ParallelBruteForceEquivalenceIT {

	/** Interrupt of each search; most searches take milliseconds, the largest of the corpus some seconds. */
	private static final long INTERRUPT_MILLIS = 120_000;

	private static final int RANDOM_SCENARIOS = 1000;

	private static final int GUILLOTINE_SCENARIOS = 500;

	private static final int MAX_BOXES = 8;

	private static final long MAX_STATES = 1_000_000;

	/** The seeds differ from those of the sweep, for other scenarios */
	private static final int SEED_OFFSET = 1_000_000;

	@Test
	void twoThreads() throws PackagerInterruptedException {
		assertAgreeWithTheReference(2);
	}

	@Test
	void fourThreads() throws PackagerInterruptedException {
		assertAgreeWithTheReference(4);
	}

	private static List<ReferenceScenario> corpus() {
		List<ReferenceScenario> scenarios = new ArrayList<>(RANDOM_SCENARIOS + GUILLOTINE_SCENARIOS);
		for (int i = 0; i < RANDOM_SCENARIOS; i++) {
			scenarios.add(ReferenceScenario.random(SEED_OFFSET + i, MAX_BOXES, MAX_STATES, i % 2 == 0));
		}
		for (int i = 0; i < GUILLOTINE_SCENARIOS; i++) {
			scenarios.add(ReferenceScenario.guillotine(SEED_OFFSET + i, MAX_BOXES, MAX_STATES));
		}
		return scenarios;
	}

	private static void assertAgreeWithTheReference(int threads) throws PackagerInterruptedException {
		// split the permutations into few work units, so that small scenarios are searched on several threads too
		int parallelizationCount = 2 * threads;

		int split = 0;
		int all = 0;
		int partial = 0;
		int nothingFits = 0;

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(threads).withParallelizationCount(parallelizationCount).build();
		ParallelBruteForcePackager skippingPackager = null;
		try {
			skippingPackager = ParallelBruteForcePackager.newBuilder().withThreads(threads).withParallelizationCount(parallelizationCount).withSkipReversePermutations(true).build();

			for (ReferenceScenario scenario : corpus()) {
				if(ReferenceComparison.fittingBoxCount(scenario) == 0) {
					// No box fits the container on its own. Through the packager's public API, as the session of the parallel packager
					// (unlike that of BruteForcePackager) throws ArrayIndexOutOfBoundsException from attempt(..) when no box fits.
					assertPublicApiPlacesNothing(packager, scenario);
					assertPublicApiPlacesNothing(skippingPackager, scenario);
					nothingFits++;
					continue;
				}
				Quality quality = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

				if(quality.boxCount() == scenario.boxCount()) {
					all++;
				} else if(quality.boxCount() > 0) {
					partial++;
				}
				// the permutations are searched on several threads for more than twice as many permutations as work units
				com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator iterator = ReferenceComparison.newReferenceIterator(scenario);
				if(iterator.length() > 0 && iterator.countPermutations() > 2L * parallelizationCount) {
					split++;
				}
			}
		} finally {
			try {
				if(skippingPackager != null) {
					skippingPackager.close();
				}
			} finally {
				packager.close();
			}
		}
		int scenarios = RANDOM_SCENARIOS + GUILLOTINE_SCENARIOS;
		assertThat(split).as("scenarios searched on several threads").isGreaterThanOrEqualTo(scenarios / 4);
		assertThat(all).as("scenarios where all boxes fit").isGreaterThanOrEqualTo(scenarios / 3);
		assertThat(partial).as("scenarios where only some boxes fit").isGreaterThanOrEqualTo(scenarios / 20);
		assertThat(nothingFits).as("scenarios where no box fits").isGreaterThanOrEqualTo(1);
	}

	/**
	 * The packager through its public API, for one container: no box fits, so nothing is packed.
	 */
	private static void assertPublicApiPlacesNothing(ParallelBruteForcePackager packager, ReferenceScenario scenario) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(scenario.newContainer(), 1)))
				.withBoxItems(scenario.newBoxItems())
				.withMaxContainerCount(1)
				.withInterruptDuration(INTERRUPT_MILLIS)
				.build();

		assertThat(result.isTimeout()).as("%s", scenario).isFalse();
		assertThat(result.isSuccess()).as("%s", scenario).isFalse();
		assertThat(result.size()).as("%s", scenario).isZero();
	}

}
