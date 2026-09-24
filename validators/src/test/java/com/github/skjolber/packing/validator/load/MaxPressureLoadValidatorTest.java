package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadPressureReason;

/**
 * Unit tests for {@link MaxPressureLoadValidator}.
 *
	 * <p>Pressure = {@code loadWeight / footprintArea}, matching the convention used by
 * {@link com.github.skjolber.packing.api.Box#getMinimumPressure()}.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class MaxPressureLoadValidatorTest {

	private final MaxPressureLoadValidator validator = new MaxPressureLoadValidator();

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

	private static Placement makePlacementWithPressureLimit(String id, int dx, int dy, int dz,
			int weight, double maxPressure, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withMaxLoadPressure(maxPressure)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// No constraint: always valid
	// -----------------------------------------------------------------------

	@Test
	void testNoConstraint_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 50, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Pressure within limit: valid
	// -----------------------------------------------------------------------

	/**
	 * Box A (10×10, area=100) has a pressure limit of 0.6.
	 * Box B (weight=50) rests on A: pressure = 50 / 100 = 0.5 ≤ 0.6 → valid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  weight=50
	 *  1  +----------+
	 *     |    A     |  10×10, maxPressure=0.6
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testWithinLimit_valid() {
		Placement a = makePlacementWithPressureLimit("A", 10, 10, 1, 20, 0.6, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 50, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exactly at limit: valid
	// -----------------------------------------------------------------------

	/**
	 * B (weight=50) on A (10×10, maxPressure=0.5): pressure = 0.5 = limit → valid.
	 */
	@Test
	void testAtLimit_valid() {
		Placement a = makePlacementWithPressureLimit("A", 10, 10, 1, 20, 0.5, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 50, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exceeds limit: invalid
	// -----------------------------------------------------------------------

	/**
	 * B (weight=50) on A (10×10, maxPressure=0.4): pressure = 0.5 > 0.4 → invalid.
	 */
	@Test
	void testExceedsLimit_invalid() {
		Placement a = makePlacementWithPressureLimit("A", 10, 10, 1, 20, 0.4, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 50, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		ExcessiveLoadPressureReason reason = (ExcessiveLoadPressureReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(a);
		assertThat(reason.getLoadPressure()).isEqualTo(0.5); // 50 / 100
		assertThat(reason.getMaxLoadPressure()).isEqualTo(0.4);
		assertThat(reason.getCode()).isEqualTo(11);
	}

	// -----------------------------------------------------------------------
	// Pressure increases area sensitivity: small footprint amplifies pressure
	// -----------------------------------------------------------------------

	/**
	 * Small box A (2×5, area=10) with B (weight=10) on top: pressure = 10/10 = 1.0.
	 * Limit = 0.999 → invalid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +--+
	 *     |B |  weight=10
	 *  1  +--+
	 *     |A |  2×5, maxPressure=0.999
	 *  0  +--+
	 * </pre>
	 */
	@Test
	void testSmallFootprintAmplifiesPressure_invalid() {
		Placement a = makePlacementWithPressureLimit("A", 2, 5, 1, 5, 0.999, 0, 0, 0);
		Placement b = makePlacement("B", 2, 5, 1, 10, 0, 0, 1);
		a.addLoad(b, 10L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		ExcessiveLoadPressureReason reason = (ExcessiveLoadPressureReason) reasons.get(0);
		assertThat(reason.getLoadPressure()).isEqualTo(1.0); // 10 / 10
	}

	// -----------------------------------------------------------------------
	// Multi-level: accumulated weight from both B and C contributes to A's pressure
	// -----------------------------------------------------------------------

	/**
	 * Three-level stack: A (10×10) → B (weight=30) → C (weight=20).
	 * Total load on A = 50; pressure = 50 / 100 = 0.5.
	 * Limit = 0.4 → invalid; limit = 0.5 → valid.
	 */
	@Test
	void testMultiLevelAccumulation_exceedsLimit_invalid() {
		Placement a = makePlacementWithPressureLimit("A", 10, 10, 1, 5, 0.4, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 30, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 20, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isFalse();
		ExcessiveLoadPressureReason reason = (ExcessiveLoadPressureReason) reasons.get(0);
		assertThat(reason.getLoadPressure()).isEqualTo(0.5);
	}

	@Test
	void testMultiLevelAccumulation_withinLimit_valid() {
		Placement a = makePlacementWithPressureLimit("A", 10, 10, 1, 5, 0.5, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 30, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 20, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}
}
