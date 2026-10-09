package com.github.skjolber.packing.packer.bruteforce.reference;

import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.NO_ROTATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.FilteredReversedBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;

/**
 * The 5.0 permutation and rotation iterator against the reference port of the 4.x one: for the same boxes and
 * container, both must enumerate the same permutation and rotation states.
 *
 * <pre>
 *   scenario --+--> reference iterator ---> states --+
 *              |                                     == (set, count, order)
 *              +--> 5.0 iterator ----------> states --+
 *              |
 *              +--> 5.0 iterator, reverse permutations skipped ---> states
 *                        subset of the reference, and every missing state has its reverse present
 * </pre>
 */
class IteratorEquivalenceTest {

	private static final int SEEDS = 60;

	private static final int MAX_BOXES = 8;

	private static final long MAX_STATES = 20_000;

	/** A permutation and rotation state */
	private record State(int[] permutations, int[] rotations, String boxes) {

		String key() {
			return ReferenceSupport.stateKey(permutations, rotations) + " " + boxes;
		}

		/** The state for the permutation in the opposite direction: the same boxes in the same rotations. */
		State reverse() {
			int n = permutations.length;
			int[] reversedPermutations = new int[n];
			int[] reversedRotations = new int[n];
			String[] boxes = this.boxes.split(";");
			String[] reversedBoxes = new String[n];
			for (int i = 0; i < n; i++) {
				reversedPermutations[i] = permutations[n - 1 - i];
				reversedRotations[i] = rotations[n - 1 - i];
				reversedBoxes[i] = boxes[n - 1 - i];
			}
			return new State(reversedPermutations, reversedRotations, String.join(";", reversedBoxes));
		}

		/** Not later than its reverse in lexicographic order, the representative of a permutation and its reverse. */
		boolean isCanonical() {
			int n = permutations.length;
			for (int i = 0; i < n / 2; i++) {
				if(permutations[i] != permutations[n - 1 - i]) {
					return permutations[i] < permutations[n - 1 - i];
				}
			}
			// a palindrome
			return true;
		}
	}

	private static String boxes(BoxItemPermutationRotationIterator iterator) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < iterator.length(); i++) {
			var stackValue = iterator.getStackValue(i);
			if(i > 0) {
				builder.append(';');
			}
			builder.append(stackValue.getBox().getId()).append('@').append(stackValue.getDx()).append('x').append(stackValue.getDy()).append('x').append(stackValue.getDz());
		}
		return builder.toString();
	}

	private static String boxes(ReferencePermutationRotationIterator iterator) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < iterator.length(); i++) {
			var stackValue = iterator.getStackValue(i);
			if(i > 0) {
				builder.append(';');
			}
			builder.append(stackValue.getBox().getId()).append('@').append(stackValue.getDx()).append('x').append(stackValue.getDy()).append('x').append(stackValue.getDz());
		}
		return builder.toString();
	}

	private static List<State> enumerate(ReferencePermutationRotationIterator iterator) {
		List<State> states = new ArrayList<>();
		if(iterator.length() == 0) {
			return states;
		}
		do {
			do {
				ReferencePermutationRotationState state = iterator.getState();
				states.add(new State(state.getPermutations(), state.getRotations(), boxes(iterator)));
			} while (iterator.nextRotation() != -1);
		} while (iterator.nextPermutation() != -1);
		return states;
	}

	private static List<State> enumerate(BoxItemPermutationRotationIterator iterator) {
		List<State> states = new ArrayList<>();
		if(iterator.length() == 0) {
			return states;
		}
		do {
			do {
				PermutationRotationState state = iterator.getState();
				states.add(new State(state.getPermutations(), state.getRotations(), boxes(iterator)));
			} while (iterator.nextRotation() != -1);
		} while (iterator.nextPermutation() != -1);
		return states;
	}

	private static ReferencePermutationRotationIterator reference(ReferenceScenario scenario) {
		Container container = scenario.newContainer();
		return ReferencePermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.withBoxItems(scenario.newBoxItems())
				.build();
	}

	private static DefaultBoxItemPermutationRotationIterator actual(ReferenceScenario scenario) {
		Container container = scenario.newContainer();
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.withBoxItems(scenario.newBoxItems())
				.build();
	}

	private static Set<String> keys(List<State> states) {
		Set<String> keys = new LinkedHashSet<>();
		for (State state : states) {
			keys.add(state.key());
		}
		return keys;
	}

	private static List<ReferenceScenario> scenarios() {
		List<ReferenceScenario> scenarios = new ArrayList<>();

		// mixed rotations, duplicates, and an oversized box which is filtered out (its index 4 is still used by the others)
		scenarios.add(new ReferenceScenario("mixed", 4, 3, 2)
				.add("a", 2, 1, 1, Rotation.THREE_D, 2)
				.add("b", 3, 2, 1, Rotation.TWO_D, 1)
				.add("c", 1, 1, 1, NO_ROTATION, 2)
				.add("big", 9, 9, 9, Rotation.THREE_D, 1)
				.add("d", 2, 2, 1, Rotation.TWO_D, 1));

		// only the rotations which fit the container are enumerated
		scenarios.add(new ReferenceScenario("constrained rotations", 5, 1, 1)
				.add("long", 1, 2, 5, Rotation.THREE_D, 1)
				.add("short", 1, 1, 2, Rotation.THREE_D, 2)
				.add("flat", 2, 1, 1, Rotation.TWO_D, 1));

		// a box which is too heavy for the container, and another with no rotation which fits
		scenarios.add(new ReferenceScenario("heavy", 4, 4, 4, 3)
				.add("light", 1, 2, 3, Rotation.THREE_D, 1, 2)
				.add("heavy", 1, 1, 1, NO_ROTATION, 4, 1)
				.add("ok", 2, 2, 2, Rotation.THREE_D, 3, 1));

		// a single kind of box
		scenarios.add(new ReferenceScenario("one kind", 3, 3, 3)
				.add("a", 1, 1, 1, Rotation.THREE_D, 7));

		// one box item, with all rotations
		scenarios.add(new ReferenceScenario("one box", 3, 3, 3)
				.add("a", 1, 2, 3, Rotation.THREE_D, 1));

		// all duplicates of a rotatable box
		scenarios.add(new ReferenceScenario("rotatable duplicates", 6, 6, 1)
				.add("a", 2, 1, 1, Rotation.TWO_D, 5));

		// nothing fits
		scenarios.add(new ReferenceScenario("nothing fits", 2, 2, 2)
				.add("a", 3, 3, 3, Rotation.THREE_D, 2));

		for (int seed = 0; seed < SEEDS; seed++) {
			scenarios.add(ReferenceScenario.random(seed, MAX_BOXES, MAX_STATES, seed % 2 == 0));
		}
		return scenarios;
	}

	@Test
	void generatedScenariosAreWithinTheBounds() {
		int excluded = 0;
		int rotated = 0;
		int duplicated = 0;
		for (ReferenceScenario scenario : scenarios()) {
			assertThat(scenario.boxCount()).as(scenario.toString()).isLessThanOrEqualTo(MAX_BOXES);
			assertThat(scenario.countStates()).as(scenario.toString()).isLessThanOrEqualTo(MAX_STATES);

			ReferencePermutationRotationIterator iterator = reference(scenario);
			if(!iterator.getExcluded().isEmpty()) {
				excluded++;
			}
			if(iterator.length() > 0 && iterator.countRotations() > 1) {
				rotated++;
			}
			if(iterator.length() > 0 && iterator.countPermutations() < factorial(iterator.length())) {
				duplicated++;
			}
		}
		// the generator covers the interesting cases, i.e. the comparison below is not vacuous
		assertThat(excluded).as("scenarios with excluded boxes").isGreaterThanOrEqualTo(5);
		assertThat(rotated).as("scenarios with rotations").isGreaterThanOrEqualTo(20);
		assertThat(duplicated).as("scenarios with duplicates").isGreaterThanOrEqualTo(10);
	}

	private static long factorial(int n) {
		long result = 1;
		for (int i = 2; i <= n; i++) {
			result *= i;
		}
		return result;
	}

	@Test
	void sameStatesAsTheReference() {
		for (ReferenceScenario scenario : scenarios()) {
			ReferencePermutationRotationIterator reference = reference(scenario);
			DefaultBoxItemPermutationRotationIterator actual = actual(scenario);

			assertThat(actual.length()).as(scenario.toString()).isEqualTo(reference.length());
			assertThat(actual.getExcluded().size()).as(scenario.toString()).isEqualTo(reference.getExcluded().size());
			if(reference.length() > 0) {
				assertThat(actual.countPermutations()).as(scenario.toString()).isEqualTo(reference.countPermutations());
				assertThat(actual.countRotations()).as(scenario.toString()).isEqualTo(reference.countRotations());
			}

			List<State> expected = enumerate(reference);
			List<State> states = enumerate(actual);

			// no state is visited twice
			assertThat(keys(expected)).as(scenario.toString()).hasSameSizeAs(expected);
			assertThat(keys(states)).as(scenario.toString()).hasSameSizeAs(states);

			assertThat(states).as(scenario.toString()).hasSameSizeAs(expected);
			assertThat(keys(states)).as(scenario.toString()).isEqualTo(keys(expected));

			if(reference.length() > 0) {
				// the number of states is that of the counts
				assertThat((long)expected.size()).as(scenario.toString()).isEqualTo(reference.countPermutations() * reference.countRotations());
			}
		}
	}

	/**
	 * The order is not what the differential tests need, but the search results (which of equally good results is
	 * kept) depend on it, and the minimum box volumes are what the search uses to discard small free points.
	 */
	@Test
	void sameOrderAndMinimumBoxVolumesAsTheReference() {
		for (ReferenceScenario scenario : scenarios()) {
			ReferencePermutationRotationIterator reference = reference(scenario);
			DefaultBoxItemPermutationRotationIterator actual = actual(scenario);
			if(reference.length() == 0) {
				continue;
			}
			int step = 0;
			do {
				do {
					String message = scenario + " step " + step++;
					ReferencePermutationRotationState expectedState = reference.getState();
					PermutationRotationState actualState = actual.getState();
					assertThat(actualState.getPermutations()).as(message).containsExactly(expectedState.getPermutations());
					assertThat(actualState.getRotations()).as(message).containsExactly(expectedState.getRotations());
					for (int i = 0; i < reference.length(); i++) {
						assertThat(actual.getMinBoxVolume(i)).as(message + " minimum volume " + i).isEqualTo(reference.getMinBoxVolume(i));
					}
					int expectedRotation = reference.nextRotation();
					assertThat(actual.nextRotation()).as(message).isEqualTo(expectedRotation);
					if(expectedRotation == -1) {
						break;
					}
				} while (true);
				int expectedPermutation = reference.nextPermutation();
				assertThat(actual.nextPermutation()).as(scenario + " step " + step).isEqualTo(expectedPermutation);
				if(expectedPermutation == -1) {
					break;
				}
			} while (true);
		}
	}

	/**
	 * The search skips permutations and rotations which cannot change the result, by passing the index of the first
	 * box which could not be placed: the same skips must lead to the same states.
	 */
	@Test
	void sameSkippingStepsAsTheReference() {
		for (ReferenceScenario scenario : scenarios()) {
			for (int seed = 0; seed < 3; seed++) {
				ReferencePermutationRotationIterator reference = reference(scenario);
				DefaultBoxItemPermutationRotationIterator actual = actual(scenario);
				if(reference.length() == 0) {
					continue;
				}
				Random random = new Random(seed);
				String message = scenario + " skip seed " + seed;
				int steps = 0;
				do {
					do {
						assertThat(actual.getState().getRotations()).as(message).containsExactly(reference.getState().getRotations());
						// the index of the first box which was not placed
						int maxIndex = random.nextInt(reference.length());
						int expected = reference.nextRotation(maxIndex);
						assertThat(actual.nextRotation(maxIndex)).as(message + " rotation step " + steps++).isEqualTo(expected);
						if(expected == -1) {
							break;
						}
					} while (true);
					assertThat(actual.getPermutations()).as(message).containsExactly(reference.getPermutations());
					int maxIndex = random.nextInt(reference.length());
					int expected = reference.nextPermutation(maxIndex);
					assertThat(actual.nextPermutation(maxIndex)).as(message + " permutation step " + steps++).isEqualTo(expected);
					if(expected == -1) {
						break;
					}
					assertThat(actual.getPermutations()).as(message).containsExactly(reference.getPermutations());
				} while (true);
			}
		}
	}

	/**
	 * Skipping reverse permutations visits one representative of each permutation and its reverse. The visited states
	 * are those of the reference with a permutation not after its reverse, and the others are missing but for their
	 * reverse, which is present: the same boxes in the same rotations, in the opposite order.
	 */
	@Test
	void skippingReversePermutationsVisitsARepresentativeOfEachState() {
		for (ReferenceScenario scenario : scenarios()) {
			List<State> expected = enumerate(reference(scenario));

			DefaultBoxItemPermutationRotationIterator delegate = actual(scenario);
			FilteredReversedBoxItemPermutationRotationIterator filtered = new FilteredReversedBoxItemPermutationRotationIterator(delegate);
			List<State> states = enumerate(filtered);

			Set<String> all = keys(expected);
			Set<String> visited = keys(states);
			String message = scenario.toString();

			// no state is visited twice
			assertThat(visited).as(message).hasSameSizeAs(states);

			// a subset of the reference
			assertThat(all).as(message).containsAll(visited);

			// exactly the states with a canonical permutation
			Set<String> canonical = new HashSet<>();
			Set<String> canonicalPermutations = new HashSet<>();
			Set<String> permutations = new HashSet<>();
			for (State state : expected) {
				permutations.add(Arrays.toString(state.permutations()));
				if(state.isCanonical()) {
					canonical.add(state.key());
					canonicalPermutations.add(Arrays.toString(state.permutations()));
				}
			}
			assertThat(visited).as(message).isEqualTo(canonical);

			// every state which is missing has its reverse
			for (State state : expected) {
				if(!visited.contains(state.key())) {
					assertThat(visited).as(message + " reverse of " + state.key()).contains(state.reverse().key());
				}
			}

			// the reverse of a missing state is not itself missing, and the states are halved at the most
			assertThat(visited.size()).as(message).isLessThanOrEqualTo(all.size());
			assertThat(visited.size()).as(message).isGreaterThanOrEqualTo((all.size() + 1) / 2);

			// the filtered iterator counts the permutations it visits
			if(!expected.isEmpty()) {
				assertThat(filtered.countPermutations()).as(message).isEqualTo(canonicalPermutations.size());
			}
		}
	}

	@Test
	void resetReturnsToTheFirstState() {
		for (ReferenceScenario scenario : scenarios()) {
			DefaultBoxItemPermutationRotationIterator actual = actual(scenario);
			ReferencePermutationRotationIterator reference = reference(scenario);
			if(reference.length() == 0) {
				continue;
			}
			List<State> first = enumerate(reference);
			reference.reset();
			actual.reset();
			assertThat(keys(enumerate(reference))).as(scenario.toString()).isEqualTo(keys(first));
			assertThat(keys(enumerate(actual))).as(scenario.toString()).isEqualTo(keys(first));
		}
	}
}
