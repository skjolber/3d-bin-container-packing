package com.github.skjolber.packing.packer.bruteforce.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.FilteredReversedBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.reference.ReferenceComparison.Quality;

/**
 * Slow integration test (run with the {@code slow-tests} profile): skipping reverse permutations in 5.0 against the
 * reference restricted to the canonical permutations, i.e. those not after their reverse. A scaled-up version of the
 * skipping checks of the fast {@link IteratorEquivalenceTest} and {@link SearchEquivalenceTest}, with up to nine boxes.
 *
 * <pre>
 *   scenario --+--> reference iterator, canonical permutations only --+
 *              |                                                      == the states, in the same order
 *              +--> 5.0 iterator, reverse permutations skipped -------+
 *              |
 *              +--> reference search, canonical permutations only ----+
 *                                                                     == quality
 *                   5.0 search, reverse permutations skipped ---------+
 * </pre>
 *
 * A permutation and its reverse pack equally well when all boxes are placed, so skipping must lose nothing when all
 * boxes fit together, and can lose the best subset only when they do not.
 * <p>
 * All searches have an interrupt deadline, so that a regression cannot hang the build: it fails the test instead.
 */
class SkipReverseSweepIT {

	/** Interrupt of each search; most searches take milliseconds, the largest of the sweep some seconds. */
	private static final long INTERRUPT_MILLIS = 120_000;

	private static final int MAX_BOXES = 9;

	/** Iterator scenarios: enumerating states costs little, so there are many of them */
	private static final int ITERATOR_SCENARIOS = 5000;

	private static final long ITERATOR_MAX_STATES = 1_000_000;

	private static final int GUILLOTINE_SCENARIOS = 10_000;

	private static final int GUILLOTINE_MAX_BOXES = 14;

	private static final long GUILLOTINE_MAX_STATES = 2_000_000;

	private static final int RANDOM_SCENARIOS = 3000;

	private static final long RANDOM_MAX_STATES = 500_000;

	/** The seeds differ from those of the other sweeps, for other scenarios */
	private static final int SEED_OFFSET = 2_000_000;

	/**
	 * The 5.0 iterator which skips reverse permutations visits the same states, in the same order, as the reference
	 * iterator which visits the canonical permutations only; these are the states of the reference with a canonical
	 * permutation, and every other state has its reverse among them.
	 */
	@Test
	void sameCanonicalStatesAsTheReference() {
		long states = 0;
		long visitedStates = 0;
		int duplicated = 0;
		for (int seed = 0; seed < ITERATOR_SCENARIOS; seed++) {
			ReferenceScenario scenario = ReferenceScenario.random(SEED_OFFSET + seed, MAX_BOXES, ITERATOR_MAX_STATES, seed % 2 == 0);
			String message = scenario.toString();

			// all states of the reference, split into those with a canonical permutation and the reverses of the others
			ReferencePermutationRotationIterator all = ReferenceComparison.newReferenceIterator(scenario);
			if(all.length() == 0) {
				continue;
			}
			Set<String> canonical = new HashSet<>();
			Set<String> reversesOfSkipped = new HashSet<>();
			long count = 0;
			do {
				do {
					ReferencePermutationRotationState state = all.getState();
					if(ReferenceSupport.isCanonical(state.getPermutations())) {
						canonical.add(ReferenceSupport.stateKey(state.getPermutations(), state.getRotations()));
					} else {
						reversesOfSkipped.add(ReferenceSupport.reverseStateKey(state.getPermutations(), state.getRotations()));
					}
					count++;
				} while (all.nextRotation() != -1);
			} while (all.nextPermutation() != -1);

			// the reference iterator which skips reverse permutations, and the 5.0 iterator
			ReferencePermutationRotationIterator expected = new ReferenceSkippingPermutationRotationIterator(ReferenceComparison.newReferenceIterator(scenario));
			Container container = scenario.newContainer();
			FilteredReversedBoxItemPermutationRotationIterator actual = new FilteredReversedBoxItemPermutationRotationIterator(
					DefaultBoxItemPermutationRotationIterator.newBuilder()
							.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
							.withMaxLoadWeight(container.getMaxLoadWeight())
							.withBoxItems(scenario.newBoxItems())
							.build());

			assertThat(actual.length()).as(message).isEqualTo(expected.length());

			Set<String> visited = new HashSet<>();
			long step = 0;
			do {
				do {
					ReferencePermutationRotationState expectedState = expected.getState();
					PermutationRotationState actualState = actual.getState();
					// plain checks, as there are millions of states: the messages are built for a failure only
					if(!Arrays.equals(actualState.getPermutations(), expectedState.getPermutations()) || !Arrays.equals(actualState.getRotations(), expectedState.getRotations())) {
						fail("%s step %d: expected %s but was %s", message, step, ReferenceSupport.stateKey(expectedState.getPermutations(), expectedState.getRotations()),
								ReferenceSupport.stateKey(actualState.getPermutations(), actualState.getRotations()));
					}
					if(!visited.add(ReferenceSupport.stateKey(expectedState.getPermutations(), expectedState.getRotations()))) {
						fail("%s step %d: state visited twice", message, step);
					}
					step++;

					int expectedRotation = expected.nextRotation();
					int actualRotation = actual.nextRotation();
					if(actualRotation != expectedRotation) {
						fail("%s step %d: next rotation expected %d but was %d", message, step, expectedRotation, actualRotation);
					}
					if(expectedRotation == -1) {
						break;
					}
				} while (true);
				int expectedPermutation = expected.nextPermutation();
				int actualPermutation = actual.nextPermutation();
				if(actualPermutation != expectedPermutation) {
					fail("%s step %d: next permutation expected %d but was %d", message, step, expectedPermutation, actualPermutation);
				}
				if(expectedPermutation == -1) {
					break;
				}
			} while (true);

			// hash set operations: assertj's collection assertions are quadratic
			if(!visited.equals(canonical)) {
				fail("%s: the states visited when skipping are not those of the reference with a canonical permutation (%d visited, %d expected)", message, visited.size(), canonical.size());
			}
			if(!visited.containsAll(reversesOfSkipped)) {
				fail("%s: a state which is skipped has no reverse among the states visited", message);
			}
			assertThat((long)visited.size()).as(message).isGreaterThanOrEqualTo((count + 1) / 2);

			states += count;
			visitedStates += visited.size();
			if(all.countPermutations() < factorial(all.length())) {
				duplicated++;
			}
		}
		// the comparison is not vacuous: many states, of which some are skipped
		assertThat(states).as("states of the reference").isGreaterThanOrEqualTo(ITERATOR_SCENARIOS * 1_000L);
		assertThat(visitedStates).as("states visited when skipping").isLessThan(states);
		assertThat(duplicated).as("scenarios with duplicate boxes").isGreaterThanOrEqualTo(ITERATOR_SCENARIOS / 10);
	}

	private static long factorial(int n) {
		long result = 1;
		for (int i = 2; i <= n; i++) {
			result *= i;
		}
		return result;
	}

	/**
	 * When all boxes fit together, skipping reverse permutations loses nothing: the reference restricted to the
	 * canonical permutations, and 5.0 skipping, place every box, like the full searches.
	 */
	@Test
	void skippingLosesNothingWhenAllBoxesFit() throws PackagerInterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build();
				BruteForcePackager skippingPackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			for (int seed = 0; seed < GUILLOTINE_SCENARIOS; seed++) {
				ReferenceScenario scenario = ReferenceScenario.guillotine(SEED_OFFSET + seed, GUILLOTINE_MAX_BOXES, GUILLOTINE_MAX_STATES);

				Quality quality = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

				Quality all = new Quality(scenario.containerVolume(), scenario.boxCount(), scenario.boxCount());
				assertThat(quality).as("%s", scenario).isEqualTo(all);
				assertThat(ReferenceComparison.reference(scenario, true, INTERRUPT_MILLIS)).as("%s, reference skipping reverse permutations", scenario).isEqualTo(all);
				assertThat(ReferenceComparison.actual(scenario, skippingPackager, true, INTERRUPT_MILLIS)).as("%s, 5.0 skipping reverse permutations", scenario).isEqualTo(all);
			}
		}
	}

	/**
	 * When only some boxes fit, skipping can lose the best subset, but it never finds a better one than the full search,
	 * and it finds what the reference restricted to the canonical permutations finds.
	 */
	@Test
	void skippingNeverBeatsTheFullSearch() throws PackagerInterruptedException {
		int partial = 0;
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build();
				BruteForcePackager skippingPackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			for (int seed = 0; seed < RANDOM_SCENARIOS; seed++) {
				ReferenceScenario scenario = ReferenceScenario.random(SEED_OFFSET + seed, MAX_BOXES, RANDOM_MAX_STATES, seed % 2 == 0);

				// the skipping search is no better than the full search, and equal to the reference which skips the same permutations
				Quality full = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

				if(full.boxCount() < ReferenceComparison.fittingBoxCount(scenario)) {
					partial++;
				}
			}
		}
		// the scenarios include searches for the best subset, where skipping can be lossy
		assertThat(partial).as("scenarios where not all boxes which fit on their own fit together").isGreaterThanOrEqualTo(RANDOM_SCENARIOS / 10);
	}

}
