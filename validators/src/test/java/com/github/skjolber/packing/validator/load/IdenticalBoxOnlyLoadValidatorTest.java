package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;

/**
 * Unit tests for {@link IdenticalBoxOnlyLoadValidator}.
 *
 * <p>Box identity is determined by {@link BoxItem} reference equality: two placements are
 * considered "identical" when their {@link Placement#getBoxItem()} returns the same instance.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class IdenticalBoxOnlyLoadValidatorTest {

	private final IdenticalBoxOnlyLoadValidator validator = new IdenticalBoxOnlyLoadValidator();

	/**
	 * Builds a box and wraps it in a {@link BoxItem} so that
	 * {@link Placement#getBoxItem()} returns a non-null value.
	 * The {@link BoxItem} constructor registers itself on the box.
	 */
	private static BoxItem makeBoxItem(String id, int dx, int dy, int dz, int weight) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withRotate2D()
				.build();
		return new BoxItem(box);
	}

	/** Creates a constrained box item that requires only identical boxes on top. */
	private static BoxItem makeIdenticalOnlyBoxItem(String id, int dx, int dy, int dz, int weight) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withMaxLoadIdenticalBoxCount(-1)  // -1 = no count limit, but identical-only flag set
				.withRotate2D()
				.build();
		return new BoxItem(box);
	}

	private static Placement makePlacement(BoxItem item, int x, int y, int z) {
		return new Placement(item.getBox().getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// No identical-only constraint: always valid regardless of box type
	// -----------------------------------------------------------------------

	/**
	 * A does not set the identical-only constraint. Any box type on top is valid.
	 */
	@Test
	void testNoConstraint_valid() {
		BoxItem itemA = makeBoxItem("A", 10, 10, 1, 20);
		BoxItem itemB = makeBoxItem("B", 10, 10, 1, 10);

		Placement a = makePlacement(itemA, 0, 0, 0);
		Placement b = makePlacement(itemB, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Nothing on top: valid
	// -----------------------------------------------------------------------

	/**
	 * A declares identical-only but has no supportees → valid.
	 */
	@Test
	void testConstrainedNothingOnTop_valid() {
		BoxItem itemA = makeIdenticalOnlyBoxItem("A", 10, 10, 1, 20);
		Placement a = makePlacement(itemA, 0, 0, 0);

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Identical box on top: valid
	// -----------------------------------------------------------------------

	/**
	 * A (identical-only) has B on top; B is of the same box type as A → valid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  same BoxItem type as A → valid
	 *  1  +----------+
	 *     |    A     |  identical-only constraint
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testIdenticalBoxOnTop_valid() {
		BoxItem itemA = makeIdenticalOnlyBoxItem("A", 10, 10, 1, 20);

		Placement a = makePlacement(itemA, 0, 0, 0);
		Placement b = makePlacement(itemA, 0, 0, 1);  // same BoxItem instance

		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Different box on top: invalid
	// -----------------------------------------------------------------------

	/**
	 * A (identical-only) has B on top; B is of a different box type → invalid.
	 */
	@Test
	void testDifferentBoxOnTop_invalid() {
		BoxItem itemA = makeIdenticalOnlyBoxItem("A", 10, 10, 1, 20);
		BoxItem itemB = makeBoxItem("B", 10, 10, 1, 10);

		Placement a = makePlacement(itemA, 0, 0, 0);
		Placement b = makePlacement(itemB, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		NonIdenticalLoadBoxReason reason = (NonIdenticalLoadBoxReason) reasons.get(0);
		assertThat(reason.getConstrainedPlacement()).isSameAs(a);
		assertThat(reason.getOffendingPlacement()).isSameAs(b);
		assertThat(reason.getExpectedBoxItem()).isSameAs(itemA);
		assertThat(reason.getActualBoxItem()).isSameAs(itemB);
		assertThat(reason.getCode()).isEqualTo(13);
	}

	// -----------------------------------------------------------------------
	// Deep stack: violation at level 2 is still detected
	// -----------------------------------------------------------------------

	/**
	 * A (identical-only) → B (same type) → C (different type). C violates the constraint.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  different type → violation detected even at level 2
	 *  2  +----------+
	 *     |    B     |  same type as A → ok at this level
	 *  1  +----------+
	 *     |    A     |  identical-only
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testDifferentBoxAtLevel2_invalid() {
		BoxItem itemA = makeIdenticalOnlyBoxItem("A", 10, 10, 1, 20);
		BoxItem itemC = makeBoxItem("C", 10, 10, 1, 5);

		Placement a = makePlacement(itemA, 0, 0, 0);
		Placement b = makePlacement(itemA, 0, 0, 1);  // same type
		Placement c = makePlacement(itemC, 0, 0, 2);  // different type

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isFalse();
		// Violation: B is the direct parent of C in the recursive check,
		// so the constrainedPlacement in the reason is B (not A).
		boolean cFoundAsOffending = reasons.stream()
				.map(r -> (NonIdenticalLoadBoxReason) r)
				.anyMatch(r -> r.getConstrainedPlacement() == b && r.getOffendingPlacement() == c);
		assertThat(cFoundAsOffending).isTrue();
	}

	// -----------------------------------------------------------------------
	// Multiple constrained placements: each is checked independently
	// -----------------------------------------------------------------------

	/**
	 * A and X both have identical-only constraints. A's supportee is a different type (violation).
	 * X's supportee is the same type (valid). Only A should produce a reason.
	 */
	@Test
	void testMultipleConstraints_onlyViolatingOneReported() {
		BoxItem itemA = makeIdenticalOnlyBoxItem("A", 10, 10, 1, 20);
		BoxItem itemB = makeBoxItem("B", 10, 10, 1, 10);   // different from A
		BoxItem itemX = makeIdenticalOnlyBoxItem("X", 10, 10, 1, 20);

		Placement a  = makePlacement(itemA, 0,  0, 0);
		Placement b  = makePlacement(itemB, 0,  0, 1);
		Placement x  = makePlacement(itemX, 0, 20, 0);
		Placement x2 = makePlacement(itemX, 0, 20, 1);  // same type as X → valid

		a.addLoad(b, 100L, b.getWeight());
		x.addLoad(x2, 100L, x2.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, x, x2), reasons)).isFalse();
		assertThat(reasons).hasSize(1);
		assertThat(((NonIdenticalLoadBoxReason) reasons.get(0)).getConstrainedPlacement()).isSameAs(a);
	}
}
