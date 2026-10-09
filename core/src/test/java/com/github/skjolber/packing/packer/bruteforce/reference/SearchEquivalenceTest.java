package com.github.skjolber.packing.packer.bruteforce.reference;

import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.NO_ROTATION;
import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.interruptAfter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeParser;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

/**
 * The 5.0 brute force search against the reference port of the 4.x recursive one, for one container: they must agree
 * on the quality of the best packing, i.e. the number of packed boxes and their total volume (and weight), not on the
 * placements: equally good placements are chosen differently.
 *
 * <pre>
 *   scenario --+--> reference recursive search ------------------> quality
 *              |                                                     ==
 *              +--> 5.0 brute force, one container, attempt ------> quality
 *              +--> 5.0 brute force, skipping reverse permutations -> quality
 * </pre>
 */
class SearchEquivalenceTest {

	/** Interrupt, so that a regression cannot hang the build; the searches take milliseconds. */
	private static final long INTERRUPT_MILLIS = 20_000;

	/** The quality of a packing: more is better, in the order of the result comparator. */
	private record Quality(long loadVolume, long loadWeight, int boxCount) {

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

	private static Quality reference(ReferenceScenario scenario) throws PackagerInterruptedException {
		return reference(scenario, false);
	}

	/**
	 * @param skipReversePermutations whether to search one of each permutation and its reverse only
	 */
	private static Quality reference(ReferenceScenario scenario, boolean skipReversePermutations) throws PackagerInterruptedException {
		Container container = scenario.newContainer();
		ReferencePermutationRotationIterator iterator = newReferenceIterator(scenario);
		if(skipReversePermutations) {
			iterator = new ReferenceSkippingPermutationRotationIterator(iterator);
		}
		ReferencePackResult result = new ReferenceRecursiveBruteForcePackager().pack(container, iterator, interruptAfter(INTERRUPT_MILLIS));

		assertPlacementsAreValid(scenario, container, result.getPlacements());
		assertThat(result.getPlacements()).as(scenario.toString()).hasSize(result.getBoxCount());

		return new Quality(result.getLoadVolume(), result.getLoadWeight(), result.getBoxCount());
	}

	private static ReferencePermutationRotationIterator newReferenceIterator(ReferenceScenario scenario) {
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
	private static int fittingBoxCount(ReferenceScenario scenario) {
		return newReferenceIterator(scenario).length();
	}

	private static Quality actual(ReferenceScenario scenario, AbstractPackager<?> packager, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		Container container = scenario.newContainer();
		PackagerInput input = new PackagerInput(scenario.newBoxItems(), null, List.of(new ContainerItem(container, 1)), 1, Order.NONE);
		PackagerSession session = packager.createSession(input, interruptAfter(INTERRUPT_MILLIS));

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

	/**
	 * The packings of the 5.0 packager through its public API, for one container: all boxes or nothing
	 */
	private static void assertPublicApiAgrees(ReferenceScenario scenario, Quality reference, boolean skipReversePermutations) {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withSkipReversePermutations(skipReversePermutations).build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(scenario.newContainer(), 1)))
					.withBoxItems(scenario.newBoxItems())
					.withMaxContainerCount(1)
					.withInterruptDuration(INTERRUPT_MILLIS)
					.build();

			String message = scenario + " public API, skip reverse permutations " + skipReversePermutations;
			assertThat(result.isTimeout()).as(message).isFalse();
			if(reference.boxCount() == scenario.boxCount()) {
				assertThat(result.isSuccess()).as(message).isTrue();
				assertThat(result.size()).as(message).isEqualTo(1);
				Stack stack = result.get(0).getStack();
				assertThat(new Quality(stack.getVolume(), stack.getWeight(), stack.size())).as(message).isEqualTo(reference);
			} else {
				// one container, and not all boxes fit
				assertThat(result.isSuccess()).as(message).isFalse();
			}
		}
	}

	/** Placements are inside the container and do not overlap. */
	private static void assertPlacementsAreValid(ReferenceScenario scenario, Container container, List<Placement> placements) {
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
	 * Compare the reference with the 5.0 brute force packager, without and with skipping reverse permutations.
	 * <p>
	 * Without skipping, the qualities must be identical. With skipping, the 5.0 search must be identical to the
	 * reference which searches the same permutations, and when the boxes which fit on their own can all be packed
	 * together, it must find that (which is what skipping is for: a caller which needs all boxes in the container
	 * asks for it, see {@link PackagerSession#attempt(int, IntermediatePackagerResult, boolean)}). When they cannot,
	 * a permutation and its reverse do not pack the same boxes, and skipping can miss the best subset.
	 *
	 * @return the quality of the reference
	 */
	private static Quality assertAgree(ReferenceScenario scenario) throws PackagerInterruptedException {
		Quality reference = reference(scenario);

		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertThat(actual(scenario, packager, false)).as("%s", scenario).isEqualTo(reference);
		}
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			Quality skipping = actual(scenario, packager, true);

			assertThat(skipping).as("%s, skipping reverse permutations, against the reference which skips them too", scenario).isEqualTo(reference(scenario, true));

			if(reference.boxCount() == fittingBoxCount(scenario)) {
				assertThat(skipping).as("%s, skipping reverse permutations, all boxes which fit fit together", scenario).isEqualTo(reference);
			} else {
				assertThat(skipping.compareTo(reference)).as("%s, skipping reverse permutations: %s, reference %s", scenario, skipping, reference).isLessThanOrEqualTo(0);
			}
		}
		return reference;
	}

	private static Quality assertAgreeAndPublicApi(ReferenceScenario scenario) throws PackagerInterruptedException {
		Quality reference = assertAgree(scenario);
		assertPublicApiAgrees(scenario, reference, false);
		assertPublicApiAgrees(scenario, reference, true);
		return reference;
	}

	// ------------------------------------------------------------------------------------------------------------
	// named instances
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * More box area than the container has (2x2, two 3x1, 2x1 and 1x1: area 13 of 9), so the best subset must be
	 * searched for. The container is filled completely by 2x2 + 3x1 + 2x1, or by 3x1 + 3x1 + 2x1 + 1x1.
	 */
	@Test
	void notEverythingFits() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("partial fit", 3, 3, 1)
				.add("square", 2, 2, 1, NO_ROTATION, 1)
				.add("long", 3, 1, 1, Rotation.TWO_D, 2)
				.add("short", 2, 1, 1, Rotation.TWO_D, 1)
				.add("unit", 1, 1, 1, NO_ROTATION, 1);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isLessThan(scenario.boxCount());
		// the container can be filled completely by several of the subsets
		assertThat(quality.loadVolume()).isEqualTo(9);
	}

	@Test
	void notEverythingFitsByWeight() throws PackagerInterruptedException {
		// room for all five boxes (volume 6 of 7), but not load weight (10 of 7). The most volume within the weight is
		// the middle box, both light boxes and one heavy box: volume 2 + 1 + 1 + 1 = 5 and weight 2 + 1 + 1 + 3 = 7,
		// where the two heavy boxes and one light box have volume 3 and weight 7
		ReferenceScenario scenario = new ReferenceScenario("partial fit by weight", 7, 1, 1, 7)
				.add("heavy", 1, 1, 1, NO_ROTATION, 3, 2)
				.add("middle", 2, 1, 1, NO_ROTATION, 2, 1)
				.add("light", 1, 1, 1, NO_ROTATION, 1, 2);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality).isEqualTo(new Quality(5, 7, 4));
	}

	@Test
	void exactFit() throws PackagerInterruptedException {
		// 2x3 + three 2x1 fill 4x3 completely
		ReferenceScenario scenario = new ReferenceScenario("exact fit", 4, 3, 1)
				.add("tall", 2, 3, 1, NO_ROTATION, 1)
				.add("bar", 2, 1, 1, Rotation.TWO_D, 3);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isEqualTo(4);
		assertThat(quality.loadVolume()).isEqualTo(scenario.containerVolume());
	}

	@Test
	void exactFitOfCubes() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("eight cubes", 2, 2, 2)
				.add("cube", 1, 1, 1, Rotation.THREE_D, 8);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isEqualTo(8);
		assertThat(quality.loadVolume()).isEqualTo(8);
	}

	@Test
	void exactFitOfThreeDimensionalSlabs() throws PackagerInterruptedException {
		// three 1x2x3 side by side, and the fourth in the remaining 3x1x3
		ReferenceScenario scenario = new ReferenceScenario("slabs", 3, 3, 3)
				.add("slab", 1, 2, 3, Rotation.THREE_D, 4);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isEqualTo(4);
		assertThat(quality.loadVolume()).isEqualTo(24);
	}

	@Test
	void duplicates() throws PackagerInterruptedException {
		// 3 + 4 + 2 boxes in several items
		ReferenceScenario scenario = new ReferenceScenario("duplicates", 4, 4, 1)
				.add("square", 2, 2, 1, NO_ROTATION, 3)
				.add("unit", 1, 1, 1, NO_ROTATION, 4)
				.add("domino", 2, 1, 1, Rotation.TWO_D, 2);

		Quality quality = assertAgreeAndPublicApi(scenario);

		// volume 12 + 4 + 4 = 20 of 16
		assertThat(quality.loadVolume()).isLessThanOrEqualTo(16);
		assertThat(quality.boxCount()).isLessThan(scenario.boxCount());
	}

	@Test
	void duplicatesWhichAllFit() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("duplicates which fit", 4, 4, 1)
				.add("square", 2, 2, 1, NO_ROTATION, 3)
				.add("unit", 1, 1, 1, NO_ROTATION, 2)
				.add("domino", 2, 1, 1, Rotation.TWO_D, 1);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isEqualTo(scenario.boxCount());
	}

	@Test
	void twoDimensionalRotationIsNeeded() throws PackagerInterruptedException {
		// three bars along x fill 3x3 of the 4x3 container, the fourth must be turned to stand in the remaining column
		ReferenceScenario scenario = new ReferenceScenario("2D rotation", 4, 3, 1)
				.add("a", 3, 1, 1, Rotation.TWO_D, 1)
				.add("b", 3, 1, 1, Rotation.TWO_D, 1)
				.add("c", 3, 1, 1, Rotation.TWO_D, 1)
				.add("d", 3, 1, 1, Rotation.TWO_D, 1);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.loadVolume()).isEqualTo(12);
		assertThat(quality.boxCount()).isEqualTo(4);
	}

	@Test
	void threeDimensionalRotationIsNeeded() throws PackagerInterruptedException {
		// only the rotations with the long side along z fit the 4x1x1 boxes; volume 20 of 16, and 16 is reached by
		// the two long boxes side by side, with the two 1x2x2 boxes lying on the 2x1x4 which remains
		ReferenceScenario scenario = new ReferenceScenario("3D rotation", 2, 2, 4)
				.add("a", 4, 1, 1, Rotation.THREE_D, 1)
				.add("b", 4, 1, 1, Rotation.THREE_D, 1)
				.add("c", 1, 2, 2, Rotation.THREE_D, 2)
				.add("d", 2, 2, 1, Rotation.THREE_D, 1);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.loadVolume()).isEqualTo(scenario.containerVolume());
		assertThat(quality.boxCount()).isLessThan(scenario.boxCount());
	}

	@Test
	void anOversizedBoxIsNeverPacked() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("oversized", 3, 3, 1)
				.add("fits", 3, 3, 1, NO_ROTATION, 1)
				.add("big", 4, 1, 1, Rotation.THREE_D, 1);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality.boxCount()).isEqualTo(1);
		assertThat(quality.loadVolume()).isEqualTo(9);
	}

	@Test
	void nothingFits() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("nothing fits", 2, 2, 2)
				.add("big", 3, 3, 3, Rotation.THREE_D, 2);

		Quality quality = assertAgreeAndPublicApi(scenario);

		assertThat(quality).isEqualTo(new Quality(0, 0, 0));
	}

	/**
	 * Skipping reverse permutations assumes that a permutation and its reverse pack equally well. They do when all
	 * boxes are packed, but not when only some are: a permutation packs a prefix of its boxes, and the prefix of the
	 * reverse is another set. Here the best packing is the 2x1x1 box alone, as the second permutation, which is the
	 * reverse of the first, so only the first permutation (1x1x1 first) is searched, and then the 2x1x1 does not fit.
	 * <p>
	 * This is why 5.0 only skips when the caller needs all boxes to fit in the container ({@code abortOnAnyBoxTooBig},
	 * set by the container strategies when only one container is left); it has no effect on the result of a packager.
	 * The test records the limitation of the search with skipping: if it is ever made exhaustive for partial packings,
	 * update this test.
	 */
	@Test
	void skippingReversePermutationsMayMissTheBestPartialPacking() throws PackagerInterruptedException {
		ReferenceScenario scenario = new ReferenceScenario("two boxes which do not fit together", 2, 1, 1)
				.add("unit", 1, 1, 1, NO_ROTATION, 1)
				.add("double", 2, 1, 1, NO_ROTATION, 1);

		// exhaustive: the 2x1x1 box, and both 5.0 and the reference agree
		Quality reference = assertAgreeAndPublicApi(scenario);
		assertThat(reference).isEqualTo(new Quality(2, 1, 1));

		// skipping: the 1x1x1 box, whether through 5.0 or the reference which skips the same permutations
		assertThat(reference(scenario, true)).isEqualTo(new Quality(1, 1, 1));
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			assertThat(actual(scenario, packager, true)).isEqualTo(new Quality(1, 1, 1));
			// not asked to abort: no skipping, and the best packing
			assertThat(actual(scenario, packager, false)).isEqualTo(new Quality(2, 1, 1));
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// squared rectangles
	// ------------------------------------------------------------------------------------------------------------

	private static ReferenceScenario bouwkamp(BouwkampCode code) {
		Map<Integer, Integer> frequencies = new TreeMap<>();
		for (BouwkampCodeLine line : code.getLines()) {
			for (Integer square : line.getSquares()) {
				frequencies.merge(square, 1, Integer::sum);
			}
		}
		// as the Bouwkamp tests of the packagers: squares of height one in a container of height one
		ReferenceScenario scenario = new ReferenceScenario("Bouwkamp " + code.getName() + " (order " + code.getOrder() + ")", code.getWidth(), code.getDepth(), 1);
		for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
			scenario.add(Integer.toString(entry.getKey()), entry.getKey(), entry.getKey(), 1, Rotation.THREE_D, entry.getValue());
		}
		return scenario;
	}

	/**
	 * The smallest squared rectangle of the test data, order 9 with duplicate squares: all boxes must be placed.
	 */
	@Test
	void bouwkampCodeFromTheTestData() throws PackagerInterruptedException {
		List<BouwkampCodes> codes = BouwkampCodeDirectory.getInstance().getSimpleImperfectSquaredRectangles(9);
		assertThat(codes).isNotEmpty();

		for (BouwkampCodes bouwkampCodes : codes) {
			for (BouwkampCode code : bouwkampCodes.getCodes()) {
				ReferenceScenario scenario = bouwkamp(code);

				Quality quality = assertAgree(scenario);

				assertThat(quality.boxCount()).as("%s", scenario).isEqualTo(code.getOrder());
				assertThat(quality.loadVolume()).as("%s", scenario).isEqualTo((long)code.getWidth() * code.getDepth());
			}
		}
	}

	/**
	 * Smaller squared rectangles in Bouwkamp notation, parsed like the test data: each tiles its rectangle, so all
	 * boxes must be placed. (The test data has none below order 9.)
	 */
	@Test
	void smallBouwkampCodes() throws PackagerInterruptedException {
		BouwkampCodeParser parser = new BouwkampCodeParser();

		// 4x3: two 2x2 on top of four 1x1
		BouwkampCode rectangle = parser.parseLine("6 4 3 (2,2)(1,1,1,1) * 6 : 4x3A");
		// 5x5: 3x3 and two 2x2 down the right, then 2x2 and four 1x1 below the 3x3
		BouwkampCode square = parser.parseLine("8 5 5 (3,2)(2)(2,1)(1,1,1) * 8 : 5x5A");

		for (BouwkampCode code : List.of(rectangle, square)) {
			ReferenceScenario scenario = bouwkamp(code);

			Quality quality = assertAgreeAndPublicApi(scenario);

			assertThat(quality.boxCount()).as("%s", scenario).isEqualTo(code.getOrder());
			assertThat(quality.loadVolume()).as("%s", scenario).isEqualTo((long)code.getWidth() * code.getDepth());
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// randomized
	// ------------------------------------------------------------------------------------------------------------

	private static final int SEEDS = 20;

	private static final int MAX_BOXES = 7;

	private static final long MAX_STATES = 5_000;

	/**
	 * Random instances with a fixed seed, with up to 7 boxes: mixed rotations, duplicates, boxes which do not fit and
	 * a load weight limit, in containers which are mostly filled or mostly empty.
	 */
	@Test
	void randomInstances() throws PackagerInterruptedException {
		int all = 0;
		int partial = 0;
		for (int seed = 0; seed < SEEDS; seed++) {
			ReferenceScenario scenario = ReferenceScenario.random(seed, MAX_BOXES, MAX_STATES, seed % 2 == 0);

			Quality quality = assertAgreeAndPublicApi(scenario);

			if(quality.boxCount() == scenario.boxCount()) {
				all++;
			} else if(quality.boxCount() > 0) {
				partial++;
			}
		}
		// the instances include both searches which place everything and searches for the best subset
		assertThat(all).as("instances where all boxes fit").isGreaterThanOrEqualTo(3);
		assertThat(partial).as("instances where only some boxes fit").isGreaterThanOrEqualTo(3);
	}

	// ------------------------------------------------------------------------------------------------------------
	// fast brute force
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Fast brute force is not exhaustive by design: for each placement it considers only the point it finds most
	 * promising. So its packing can be worse than the reference's, but never better.
	 */
	@Test
	void fastBruteForceIsNoBetterThanTheExhaustiveReference() throws PackagerInterruptedException {
		List<ReferenceScenario> scenarios = new ArrayList<>();
		scenarios.add(new ReferenceScenario("partial fit", 3, 3, 1)
				.add("square", 2, 2, 1, NO_ROTATION, 1)
				.add("long", 3, 1, 1, Rotation.TWO_D, 2)
				.add("short", 2, 1, 1, Rotation.TWO_D, 1)
				.add("unit", 1, 1, 1, NO_ROTATION, 1));
		for (int seed = 0; seed < 10; seed++) {
			scenarios.add(ReferenceScenario.random(seed, MAX_BOXES, MAX_STATES, seed % 2 == 0));
		}

		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			for (ReferenceScenario scenario : scenarios) {
				Quality reference = reference(scenario);
				Quality fast = actual(scenario, packager, false);

				assertThat(reference.compareTo(fast)).as("%s: reference %s, fast %s", scenario, reference, fast).isGreaterThanOrEqualTo(0);
			}
		}
	}

}
