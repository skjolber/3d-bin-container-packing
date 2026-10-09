package com.github.skjolber.packing.packer.bruteforce.reference;

import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.interruptAfter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * Running the reference search and the 5.0 searches on a {@link ReferenceScenario}, and comparing the quality of the best
 * packing they find. Shared by the fast differential test and the slow integration tests.
 * <p>
 * All searches take an interrupt duration, so that a regression cannot hang the build.
 */

final class ReferenceComparison {

	private ReferenceComparison() {
	}

	/** The quality of a packing: more is better, in the order of the result comparator. */
	record Quality(long loadVolume, long loadWeight, int boxCount) {

		/** @return positive if this packing is better than the other, negative if worse */
		int compareTo(Quality other) {
			int compare = Long.compare(loadVolume, other.loadVolume);
			if(compare != 0) {
				return compare;
			}
			compare = Long.compare(loadWeight, other.loadWeight);
			if(compare != 0) {
				return compare;
			}
			return Integer.compare(boxCount, other.boxCount);
		}
	}

	static ReferencePermutationRotationIterator newReferenceIterator(ReferenceScenario scenario) {
		Container container = scenario.newContainer();
		return ReferencePermutationRotationIterator
				.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(scenario.newBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
	}

	/**
	 * @return the number of boxes which fit the container on their own (by size and weight)
	 */
	static int fittingBoxCount(ReferenceScenario scenario) {
		return newReferenceIterator(scenario).length();
	}

	/**
	 * @param scenario the scenario
	 * @param skipReversePermutations whether to search one of each permutation and its reverse only
	 * @param interruptMillis interrupt duration
	 * @return the quality of the best packing of the reference search
	 */
	static Quality reference(ReferenceScenario scenario, boolean skipReversePermutations, long interruptMillis) throws PackagerInterruptedException {
		Container container = scenario.newContainer();
		ReferencePermutationRotationIterator iterator = newReferenceIterator(scenario);
		if(skipReversePermutations) {
			iterator = new ReferenceSkippingPermutationRotationIterator(iterator);
		}
		ReferencePackResult result = new ReferenceRecursiveBruteForcePackager().pack(container, iterator, interruptAfter(interruptMillis));

		assertPlacementsAreValid(scenario, container, result.getPlacements());
		assertThat(result.getPlacements()).as(scenario.toString()).hasSize(result.getBoxCount());

		return new Quality(result.getLoadVolume(), result.getLoadWeight(), result.getBoxCount());
	}

	/**
	 * @param scenario the scenario
	 * @param packager the packager
	 * @param abortOnAnyBoxTooBig whether the caller needs all boxes to fit; skipping reverse permutations is only done then
	 * @param interruptMillis interrupt duration
	 * @return the quality of the best packing into the one container, whether or not all boxes fit
	 */
	static Quality actual(ReferenceScenario scenario, AbstractPackager<?> packager, boolean abortOnAnyBoxTooBig, long interruptMillis) throws PackagerInterruptedException {
		Container container = scenario.newContainer();
		PackagerInput input = new PackagerInput(scenario.newBoxItems(), null, List.of(new ContainerItem(container, 1)), 1, Order.NONE);
		PackagerSession session = packager.createSession(input, interruptAfter(interruptMillis));

		// the best packing into the container, whether or not all boxes fit. Skipping reverse permutations is only
		// done when the caller asks to abort on boxes which are too big, i.e. when it needs all boxes to fit
		IntermediatePackagerResult result = session.attempt(0, null, abortOnAnyBoxTooBig);
		if(result == null || result.isEmpty()) {
			return new Quality(0, 0, 0);
		}
		Stack stack = result.getStack();

		assertPlacementsAreValid(scenario, container, stack.getPlacements());

		return new Quality(stack.getVolume(), stack.getWeight(), stack.size());
	}

	/** Placements are inside the container and do not overlap. */
	static void assertPlacementsAreValid(ReferenceScenario scenario, Container container, List<Placement> placements) {
		for (int i = 0; i < placements.size(); i++) {
			Placement a = placements.get(i);
			assertThat(a.getAbsoluteX()).as("%s: %s", scenario, a).isGreaterThanOrEqualTo(0);
			assertThat(a.getAbsoluteY()).as("%s: %s", scenario, a).isGreaterThanOrEqualTo(0);
			assertThat(a.getAbsoluteZ()).as("%s: %s", scenario, a).isGreaterThanOrEqualTo(0);
			assertThat(a.getAbsoluteEndX()).as("%s: %s", scenario, a).isLessThan(container.getLoadDx());
			assertThat(a.getAbsoluteEndY()).as("%s: %s", scenario, a).isLessThan(container.getLoadDy());
			assertThat(a.getAbsoluteEndZ()).as("%s: %s", scenario, a).isLessThan(container.getLoadDz());
			for (int j = i + 1; j < placements.size(); j++) {
				assertThat(a.intersects3D(placements.get(j))).as("%s: %s and %s intersect", scenario, a, placements.get(j)).isFalse();
			}
		}
	}

	/**
	 * Compare the reference with 5.0 packagers, without and with skipping reverse permutations.
	 * <p>
	 * Without skipping, the qualities must be identical. With skipping, the 5.0 search must be identical to the
	 * reference which searches the same permutations, and when the boxes which fit on their own can all be packed
	 * together, it must find that (which is what skipping is for: a caller which needs all boxes in the container
	 * asks for it, see {@link PackagerSession#attempt(int, IntermediatePackagerResult, boolean)}). When they cannot,
	 * a permutation and its reverse do not pack the same boxes, and skipping can miss the best subset.
	 *
	 * @param scenario the scenario
	 * @param packager packager which does not skip reverse permutations
	 * @param skippingPackager packager which skips reverse permutations
	 * @param interruptMillis interrupt duration of each search
	 * @return the quality of the reference
	 */
	static Quality assertAgree(ReferenceScenario scenario, AbstractPackager<?> packager, AbstractPackager<?> skippingPackager, long interruptMillis)
			throws PackagerInterruptedException {
		Quality reference = reference(scenario, false, interruptMillis);

		assertThat(actual(scenario, packager, false, interruptMillis)).as("%s", scenario).isEqualTo(reference);

		Quality skipping = actual(scenario, skippingPackager, true, interruptMillis);

		assertThat(skipping).as("%s, skipping reverse permutations, against the reference which skips them too", scenario).isEqualTo(reference(scenario, true, interruptMillis));

		if(reference.boxCount() == fittingBoxCount(scenario)) {
			assertThat(skipping).as("%s, skipping reverse permutations, all boxes which fit fit together", scenario).isEqualTo(reference);
		} else {
			assertThat(skipping.compareTo(reference)).as("%s, skipping reverse permutations: %s, reference %s", scenario, skipping, reference).isLessThanOrEqualTo(0);
		}
		return reference;
	}

}
