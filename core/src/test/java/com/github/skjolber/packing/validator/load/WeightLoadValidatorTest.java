package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;

/**
 * Unit tests for {@link WeightLoadValidator}.
 *
 * <p>Load weight is accumulated by traversing the supportee graph; no cached
 * {@code loadWeight} field is used.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class WeightLoadValidatorTest {

	private final WeightLoadValidator validator = new WeightLoadValidator();

	/** Creates a plain placement with no load constraint. */
	private static Placement makePlacement(String id, int dx, int dy, int dz, int weight,
			int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	/** Creates a placement whose stack value carries a max-load-weight constraint. */
	private static Placement makePlacementWithWeightLimit(String id, int dx, int dy, int dz,
			int weight, long maxLoadWeight, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withMaxLoadWeight(maxLoadWeight)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// No constraint: always valid
	// -----------------------------------------------------------------------

	/**
	 * Stack values without a max-load-weight constraint are never checked.
	 */
	@Test
	void testNoConstraint_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Load within limit: valid
	// -----------------------------------------------------------------------

	/**
	 * Box A can bear up to 15 units; B (weight=10) rests on A → loadWeight=10 ≤ 15 → valid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  weight=10
	 *  1  +----------+
	 *     |    A     |  maxLoadWeight=15
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testWithinLimit_valid() {
		Placement a = makePlacementWithWeightLimit("A", 10, 10, 1, 20, 15L, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exactly at limit: valid
	// -----------------------------------------------------------------------

	@Test
	void testAtLimit_valid() {
		Placement a = makePlacementWithWeightLimit("A", 10, 10, 1, 20, 10L, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exceeds limit: invalid
	// -----------------------------------------------------------------------

	/**
	 * Box A can bear up to 8 units; B (weight=10) rests on A → loadWeight=10 > 8 → invalid.
	 */
	@Test
	void testExceedsLimit_invalid() {
		Placement a = makePlacementWithWeightLimit("A", 10, 10, 1, 20, 8L, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		ExcessiveLoadWeightReason reason = (ExcessiveLoadWeightReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(a);
		assertThat(reason.getLoadWeight()).isEqualTo(10L);
		assertThat(reason.getMaxLoadWeight()).isEqualTo(8L);
		assertThat(reason.getCode()).isEqualTo(10);
	}

	// -----------------------------------------------------------------------
	// Multi-level stack: accumulated weight propagates correctly
	// -----------------------------------------------------------------------

	/**
	 * Three-level stack A → B → C. A bears the weight of both B and C.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  weight=5
	 *  2  +----------+
	 *     |    B     |  weight=10
	 *  1  +----------+
	 *     |    A     |  maxLoadWeight=20; accumulated load = 10+5=15 ≤ 20 → valid
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testThreeLevelStack_accumulatesCorrectly_valid() {
		Placement a = makePlacementWithWeightLimit("A", 10, 10, 1, 30, 20L, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	/**
	 * Same three-level stack; A's limit is only 14 — accumulated weight (15) exceeds it.
	 */
	@Test
	void testThreeLevelStack_accumulatedWeightExceedsLimit_invalid() {
		Placement a = makePlacementWithWeightLimit("A", 10, 10, 1, 30, 14L, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		ExcessiveLoadWeightReason reason = (ExcessiveLoadWeightReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(a);
		assertThat(reason.getLoadWeight()).isEqualTo(15L);
		assertThat(reason.getMaxLoadWeight()).isEqualTo(14L);
	}

	// -----------------------------------------------------------------------
	// Split load: weight shared proportionally
	// -----------------------------------------------------------------------

	/**
	 * Box C (weight=10) rests equally on A and B (50% each → each bears 5 units).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +-----+-----+
	 *     |      C     |  weight=10
	 *  1  +-----+-----+
	 *     |  A  |  B  |  A: maxLoadWeight=4, B: maxLoadWeight=6
	 *  0  +-----+-----+
	 *     0     5     10  x
	 * </pre>
	 *
	 * A bears 5 > 4 → A invalid. B bears 5 ≤ 6 → B valid.
	 */
	@Test
	void testSplitLoad_oneExceedsLimit() {
		Placement a = makePlacementWithWeightLimit("A", 5, 10, 1, 10, 4L, 0, 0, 0);
		Placement b = makePlacementWithWeightLimit("B", 5, 10, 1, 10, 6L, 5, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 1);

		a.addLoad(c, 50L, 5L);
		b.addLoad(c, 50L, 5L);

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isFalse();
		assertThat(reasons).hasSize(1);
		assertThat(((ExcessiveLoadWeightReason) reasons.get(0)).getPlacement()).isSameAs(a);
	}
}
