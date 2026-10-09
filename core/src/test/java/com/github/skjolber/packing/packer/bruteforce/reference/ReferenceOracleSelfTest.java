package com.github.skjolber.packing.packer.bruteforce.reference;

import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.NO_ROTATION;
import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.container;
import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.interruptAfter;
import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.item;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

/**
 * Hand-computable counts and results of the reference implementation. The differential tests are only meaningful
 * if these pass: they pin the oracle itself.
 */
class ReferenceOracleSelfTest {

	/** The enumeration of an iterator in the do-while approach: every permutation, with every rotation state of it. */
	private static class Enumeration {

		private final List<String> permutations = new ArrayList<>();
		private final List<Integer> rotationStatesPerPermutation = new ArrayList<>();
		private final List<String> states = new ArrayList<>();

		private static Enumeration of(ReferencePermutationRotationIterator iterator) {
			Enumeration enumeration = new Enumeration();
			do {
				enumeration.permutations.add(Arrays.toString(iterator.getPermutations()));
				int rotationStates = 0;
				do {
					rotationStates++;
					ReferencePermutationRotationState state = iterator.getState();
					enumeration.states.add(ReferenceSupport.stateKey(state.getPermutations(), state.getRotations()));
				} while (iterator.nextRotation() != -1);
				enumeration.rotationStatesPerPermutation.add(rotationStates);
			} while (iterator.nextPermutation() != -1);
			return enumeration;
		}
	}

	private static ReferencePermutationRotationIterator iterator(Container container, BoxItem... items) {
		return ReferencePermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.withBoxItems(Arrays.asList(items))
				.build();
	}

	private static final Container ROOMY = container(10, 10, 10);

	// ------------------------------------------------------------------------------------------------------------
	// enumeration
	// ------------------------------------------------------------------------------------------------------------

	@Test
	void threeDistinctBoxesWithoutRotationHaveSixPermutationsAndOneRotationStateEach() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 1),
				item("b", 2, 1, 1, NO_ROTATION, 1),
				item("c", 3, 1, 1, NO_ROTATION, 1));

		assertThat(iterator.countPermutations()).isEqualTo(6);
		assertThat(iterator.countRotations()).isEqualTo(1);

		Enumeration enumeration = Enumeration.of(iterator);

		// 3! in lexicographic order
		assertThat(enumeration.permutations).containsExactly("[0, 1, 2]", "[0, 2, 1]", "[1, 0, 2]", "[1, 2, 0]", "[2, 0, 1]", "[2, 1, 0]");
		assertThat(enumeration.rotationStatesPerPermutation).containsExactly(1, 1, 1, 1, 1, 1);
		assertThat(enumeration.states).hasSize(6).doesNotHaveDuplicates();
	}

	@Test
	void oneTwoDimensionalRotatableNonSquareBoxDoublesTheRotationStates() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("rotatable", 2, 1, 1, Rotation.TWO_D, 1),
				item("b", 2, 1, 1, NO_ROTATION, 1),
				item("c", 3, 1, 1, NO_ROTATION, 1));

		assertThat(iterator.countPermutations()).isEqualTo(6);
		assertThat(iterator.countRotations()).isEqualTo(2);

		Enumeration enumeration = Enumeration.of(iterator);

		assertThat(enumeration.permutations).hasSize(6);
		assertThat(enumeration.rotationStatesPerPermutation).containsExactly(2, 2, 2, 2, 2, 2);
		assertThat(enumeration.states).hasSize(12).doesNotHaveDuplicates();
	}

	@Test
	void aCubeAddsNoRotationStates() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("cube", 1, 1, 1, Rotation.TWO_D, 1),
				item("b", 2, 1, 1, NO_ROTATION, 1),
				item("c", 3, 1, 1, NO_ROTATION, 1));

		assertThat(iterator.countRotations()).isEqualTo(1);

		Enumeration enumeration = Enumeration.of(iterator);

		assertThat(enumeration.rotationStatesPerPermutation).containsExactly(1, 1, 1, 1, 1, 1);
		assertThat(enumeration.states).hasSize(6);
	}

	@Test
	void threeDimensionalRotationOfABoxWithThreeDistinctSidesHasSixRotationStates() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY, item("a", 1, 2, 3, Rotation.THREE_D, 1));

		assertThat(iterator.countRotations()).isEqualTo(6);

		Enumeration enumeration = Enumeration.of(iterator);

		assertThat(enumeration.permutations).containsExactly("[0]");
		assertThat(enumeration.rotationStatesPerPermutation).containsExactly(6);
	}

	/**
	 * The rule of the 4.x iterator: the permutations are those of the multiset of box item indexes. A box item with a
	 * count above one occupies several positions, and its copies are not permuted among themselves, so the permutation
	 * count is n! / (count(0)! * count(1)! * ...), in lexicographic order.
	 */
	@Test
	void duplicateBoxesAreMultisetPermutations() {
		// a, a, b: 3!/2! = 3
		ReferencePermutationRotationIterator two = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 2),
				item("b", 2, 1, 1, NO_ROTATION, 1));
		assertThat(two.countPermutations()).isEqualTo(3);
		assertThat(Enumeration.of(two).permutations).containsExactly("[0, 0, 1]", "[0, 1, 0]", "[1, 0, 0]");

		// a, a, b, b: 4!/(2!*2!) = 6
		ReferencePermutationRotationIterator twoTwo = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 2),
				item("b", 2, 1, 1, NO_ROTATION, 2));
		assertThat(twoTwo.countPermutations()).isEqualTo(6);
		assertThat(Enumeration.of(twoTwo).permutations).hasSize(6).doesNotHaveDuplicates();

		// a, a, a, b, c: 5!/3! = 20
		ReferencePermutationRotationIterator three = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 3),
				item("b", 2, 1, 1, NO_ROTATION, 1),
				item("c", 3, 1, 1, NO_ROTATION, 1));
		assertThat(three.countPermutations()).isEqualTo(20);
		assertThat(Enumeration.of(three).permutations).hasSize(20).doesNotHaveDuplicates();

		// a, a, a, b, b, b: 6!/(3!*3!) = 20
		ReferencePermutationRotationIterator threeThree = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 3),
				item("b", 2, 1, 1, NO_ROTATION, 3));
		assertThat(threeThree.countPermutations()).isEqualTo(20);
		assertThat(Enumeration.of(threeThree).permutations).hasSize(20).doesNotHaveDuplicates();
	}

	/**
	 * Identical boxes in different box items are different indexes: no merging by box equality, only by count.
	 */
	@Test
	void identicalBoxesInSeparateBoxItemsAreNotMerged() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("same", 1, 1, 1, NO_ROTATION, 1),
				item("same", 1, 1, 1, NO_ROTATION, 1));

		assertThat(iterator.countPermutations()).isEqualTo(2);
		assertThat(Enumeration.of(iterator).permutations).containsExactly("[0, 1]", "[1, 0]");
	}

	/**
	 * Rotations are enumerated independently for each position, also for copies of the same box: the rotation states
	 * of duplicates are mirrored, not collapsed.
	 */
	@Test
	void rotationsOfDuplicatesAreEnumeratedIndependently() {
		// a, a with two rotations each: one permutation, 2*2 rotation states
		ReferencePermutationRotationIterator iterator = iterator(ROOMY, item("a", 2, 1, 1, Rotation.TWO_D, 2));

		assertThat(iterator.countPermutations()).isEqualTo(1);
		assertThat(iterator.countRotations()).isEqualTo(4);

		Enumeration enumeration = Enumeration.of(iterator);
		assertThat(enumeration.permutations).containsExactly("[0, 0]");
		assertThat(enumeration.states).containsExactly("[0, 0]/[0, 0]", "[0, 0]/[0, 1]", "[0, 0]/[1, 0]", "[0, 0]/[1, 1]");

		// plus b: 3 permutations * 4 rotation states
		ReferencePermutationRotationIterator withOther = iterator(ROOMY,
				item("a", 2, 1, 1, Rotation.TWO_D, 2),
				item("b", 3, 1, 1, NO_ROTATION, 1));
		assertThat(Enumeration.of(withOther).states).hasSize(12).doesNotHaveDuplicates();
	}

	@Test
	void aBoxWhichDoesNotFitTheContainerIsExcludedFromTheEnumeration() {
		Container container = container(3, 3, 3, 10);

		BoxItem fits = item("fits", 1, 1, 1, NO_ROTATION, 1);
		// volume 125 > 27
		BoxItem tooBig = item("tooBig", 5, 5, 5, NO_ROTATION, 1);
		// volume 4 <= 27, but one side is 4 > 3 in every rotation
		BoxItem tooLong = new BoxItem(Box.newBuilder().withId("tooLong").withSize(1, 1, 4).withRotate3D().withWeight(1).build(), 1);
		// weight 11 > 10
		BoxItem tooHeavy = new BoxItem(Box.newBuilder().withId("tooHeavy").withSize(1, 1, 1).withRotation(NO_ROTATION).withWeight(11).build(), 1);
		BoxItem alsoFits = item("alsoFits", 2, 1, 1, NO_ROTATION, 1);

		ReferencePermutationRotationIterator iterator = iterator(container, fits, tooBig, tooLong, tooHeavy, alsoFits);

		assertThat(iterator.getExcluded()).containsExactly(tooBig, tooLong, tooHeavy);
		assertThat(iterator.length()).isEqualTo(2);
		assertThat(iterator.countPermutations()).isEqualTo(2);

		// the box items keep their index in the input
		assertThat(Enumeration.of(iterator).permutations).containsExactly("[0, 4]", "[4, 0]");
	}

	@Test
	void allBoxesExcludedLeavesNothingToEnumerate() {
		ReferencePermutationRotationIterator iterator = iterator(container(1, 1, 1), item("tooBig", 2, 2, 2, NO_ROTATION, 3));

		assertThat(iterator.length()).isZero();
		assertThat(iterator.getExcluded()).hasSize(1);
	}

	/**
	 * nextPermutation(maxIndex) skips the permutations which share the prefix up to and including the index.
	 */
	@Test
	void nextPermutationWithMaxIndexSkipsPermutationsWhichShareThePrefix() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("a", 1, 1, 1, NO_ROTATION, 1),
				item("b", 2, 1, 1, NO_ROTATION, 1),
				item("c", 3, 1, 1, NO_ROTATION, 1),
				item("d", 4, 1, 1, NO_ROTATION, 1));

		assertThat(iterator.getPermutations()).containsExactly(0, 1, 2, 3);

		// the box at index 1 could not be placed: [0, 1, 3, 2] only reorders the boxes after it
		assertThat(iterator.nextPermutation(1)).isEqualTo(1);
		assertThat(iterator.getPermutations()).containsExactly(0, 2, 1, 3);

		// the box at index 0 could not be placed
		assertThat(iterator.nextPermutation(0)).isEqualTo(0);
		assertThat(iterator.getPermutations()).containsExactly(1, 0, 2, 3);

		assertThat(iterator.nextPermutation(0)).isEqualTo(0);
		assertThat(iterator.getPermutations()).containsExactly(2, 0, 1, 3);

		assertThat(iterator.nextPermutation(0)).isEqualTo(0);
		assertThat(iterator.getPermutations()).containsExactly(3, 0, 1, 2);

		// no larger box to put first: no more permutations
		assertThat(iterator.nextPermutation(0)).isEqualTo(-1);
	}

	@Test
	void nextRotationWithMaxIndexOnlyRotatesUpToTheIndex() {
		ReferencePermutationRotationIterator iterator = iterator(ROOMY,
				item("a", 2, 1, 1, Rotation.TWO_D, 1),
				item("b", 3, 1, 1, Rotation.TWO_D, 1),
				item("c", 4, 1, 1, Rotation.TWO_D, 1));

		assertThat(iterator.getState().getRotations()).containsExactly(0, 0, 0);

		// rotating only boxes 0 and 1
		assertThat(iterator.nextRotation(1)).isEqualTo(1);
		assertThat(iterator.getState().getRotations()).containsExactly(0, 1, 0);

		assertThat(iterator.nextRotation(1)).isEqualTo(0);
		assertThat(iterator.getState().getRotations()).containsExactly(1, 0, 0);

		assertThat(iterator.nextRotation(1)).isEqualTo(1);
		assertThat(iterator.getState().getRotations()).containsExactly(1, 1, 0);

		assertThat(iterator.nextRotation(1)).isEqualTo(-1);
	}

	// ------------------------------------------------------------------------------------------------------------
	// search
	// ------------------------------------------------------------------------------------------------------------

	private static ReferencePackResult pack(Container container, BoxItem... items) throws PackagerInterruptedException {
		return new ReferenceRecursiveBruteForcePackager().pack(container, Arrays.asList(items), interruptAfter(10_000));
	}

	@Test
	void twoUnitCubesFitAContainerOfTwoUnits() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(2, 1, 1), item("cube", 1, 1, 1, NO_ROTATION, 2));

		assertThat(result.getBoxCount()).isEqualTo(2);
		assertThat(result.getLoadVolume()).isEqualTo(2);
		assertThat(result.getLoadWeight()).isEqualTo(2);

		List<Placement> placements = result.getPlacements();
		assertThat(placements).hasSize(2);
		assertThat(positions(placements)).containsExactlyInAnyOrder("0,0,0", "1,0,0");
	}

	@Test
	void eightUnitCubesFitAContainerOfTwoByTwoByTwo() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(2, 2, 2), item("cube", 1, 1, 1, NO_ROTATION, 8));

		assertThat(result.getBoxCount()).isEqualTo(8);
		assertThat(result.getLoadVolume()).isEqualTo(8);
		assertThat(positions(result.getPlacements())).containsExactlyInAnyOrder("0,0,0", "1,0,0", "0,1,0", "1,1,0", "0,0,1", "1,0,1", "0,1,1", "1,1,1");
	}

	@Test
	void boxesBeyondTheContainerAreNotPlaced() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(2, 1, 1), item("cube", 1, 1, 1, NO_ROTATION, 3));

		assertThat(result.getBoxCount()).isEqualTo(2);
		assertThat(result.getLoadVolume()).isEqualTo(2);
	}

	@Test
	void theMaximumLoadWeightOfTheContainerLimitsTheBoxes() throws PackagerInterruptedException {
		// room for 3, load weight for 2
		ReferencePackResult result = pack(container(3, 1, 1, 2), item("cube", 1, 1, 1, NO_ROTATION, 3));

		assertThat(result.getBoxCount()).isEqualTo(2);
		assertThat(result.getLoadWeight()).isEqualTo(2);
	}

	@Test
	void aBoxIsRotatedWhenItDoesNotFitOtherwise() throws PackagerInterruptedException {
		// as given 3x1x1, the container is 1x3x1
		ReferencePackResult result = pack(container(1, 3, 1), item("long", 3, 1, 1, Rotation.TWO_D, 1));

		assertThat(result.getBoxCount()).isEqualTo(1);
		Placement placement = result.getPlacements().get(0);
		assertThat(placement.getStackValue().getDx()).isEqualTo(1);
		assertThat(placement.getStackValue().getDy()).isEqualTo(3);
	}

	/**
	 * The first permutation (3, 2, 2) places only the 3 (volume 3); the permutation (2, 2, 3) places the two 2s (volume 4).
	 */
	@Test
	void anotherPermutationCanBeBetter() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(4, 1, 1),
				item("three", 3, 1, 1, NO_ROTATION, 1),
				item("two", 2, 1, 1, NO_ROTATION, 2));

		assertThat(result.getBoxCount()).isEqualTo(2);
		assertThat(result.getLoadVolume()).isEqualTo(4);
	}

	/**
	 * Across permutations, load volume is compared before the number of boxes: (4, 1) places two boxes with volume 5,
	 * while (1, 1, 1, 4) places three boxes with volume 3.
	 */
	@Test
	void resultsOfDifferentPermutationsAreComparedByVolumeBeforeCount() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(5, 1, 1),
				item("four", 4, 1, 1, NO_ROTATION, 1),
				item("one", 1, 1, 1, NO_ROTATION, 3));

		assertThat(result.getBoxCount()).isEqualTo(2);
		assertThat(result.getLoadVolume()).isEqualTo(5);
	}

	@Test
	void anEmptyContainerOfBoxesGivesAnEmptyResult() throws PackagerInterruptedException {
		ReferencePackResult result = pack(container(1, 1, 1), item("tooBig", 2, 2, 2, NO_ROTATION, 1));

		assertThat(result.isEmpty()).isTrue();
		assertThat(result.getBoxCount()).isZero();
		assertThat(result.getLoadVolume()).isZero();
		assertThat(result.getPlacements()).isEmpty();
	}

	@Test
	void theInputBoxItemsAreNotChanged() throws PackagerInterruptedException {
		BoxItem item = item("cube", 1, 1, 1, NO_ROTATION, 2);

		pack(container(2, 1, 1), item);
		pack(container(2, 1, 1), item);

		assertThat(item.getCount()).isEqualTo(2);
	}

	@Test
	void anInterruptIsPropagated() {
		org.junit.jupiter.api.Assertions.assertThrows(PackagerInterruptedException.class,
				() -> new ReferenceRecursiveBruteForcePackager().pack(container(2, 1, 1), Arrays.asList(item("cube", 1, 1, 1, NO_ROTATION, 2)), () -> true));
	}

	private static List<String> positions(List<Placement> placements) {
		List<String> positions = new ArrayList<>();
		for (Placement placement : placements) {
			positions.add(placement.getAbsoluteX() + "," + placement.getAbsoluteY() + "," + placement.getAbsoluteZ());
		}
		return positions;
	}

}
