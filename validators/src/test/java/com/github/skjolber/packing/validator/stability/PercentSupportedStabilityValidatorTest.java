package com.github.skjolber.packing.validator.stability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * Unit tests for {@link PercentSupportedStabilityValidator}.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class PercentSupportedStabilityValidatorTest {

	private static Placement makePlacement(String id, int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(1)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// Constructor validation
	// -----------------------------------------------------------------------

	@Test
	void testConstructor_negativePct_throws() {
		assertThatThrownBy(() -> new PercentSupportedStabilityValidator(-1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testConstructor_greaterThan100_throws() {
		assertThatThrownBy(() -> new PercentSupportedStabilityValidator(101))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testConstructor_zeroPct_ok() {
		assertThat(new PercentSupportedStabilityValidator(0).getMinPercent()).isEqualTo(0);
	}

	@Test
	void testConstructor_100Pct_ok() {
		assertThat(new PercentSupportedStabilityValidator(100).getMinPercent()).isEqualTo(100);
	}

	// -----------------------------------------------------------------------
	// Floor placement: always valid regardless of threshold
	// -----------------------------------------------------------------------

	/**
	 * A floor placement has {@code z == 0} and is always considered fully supported.
	 */
	@Test
	void testFloorPlacement_threshold100_valid() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(100);
		Placement a = makePlacement("A", 10, 10, 1, 0, 0, 0);

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// 100% coverage
	// -----------------------------------------------------------------------

	/**
	 * Box B fully supported (100%), threshold=100 → valid.
	 */
	@Test
	void testFullSupport_threshold100_valid() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(100);

		Placement a = makePlacement("A", 10, 10, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exactly at threshold: valid
	// -----------------------------------------------------------------------

	/**
	 * Box B (10×10) supported over 50 units (50%), threshold=50 → valid (≥ threshold).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  supportedArea=50, area=100 → 50% ≥ 50 → valid
	 *  1  +-----+
	 *     |  A  |
	 *  0  +-----+
	 *     0     5    10  x
	 * </pre>
	 */
	@Test
	void testAtThreshold_valid() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(50);

		Placement a = makePlacement("A",  5, 10, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);
		a.addLoad(b, 50L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Just below threshold: invalid
	// -----------------------------------------------------------------------

	/**
	 * Box B supported over 50 units (50%), threshold=51 → invalid.
	 */
	@Test
	void testBelowThreshold_invalid() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(51);

		Placement a = makePlacement("A",  5, 10, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);
		a.addLoad(b, 50L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		InsufficientSupportAreaReason reason = (InsufficientSupportAreaReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(b);
		assertThat(reason.getSupportedArea()).isEqualTo(50L);
		// requiredArea = (footprintArea * minPercent) / 100 = (100 * 51) / 100 = 51
		assertThat(reason.getRequiredArea()).isEqualTo(51L);
		assertThat(reason.getCode()).isEqualTo(20);
	}

	// -----------------------------------------------------------------------
	// Zero percent threshold: everything elevated is valid
	// -----------------------------------------------------------------------

	/**
	 * With threshold=0, any elevated box (even with no supporters) is valid.
	 */
	@Test
	void testZeroThreshold_elevated_valid() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(b), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Multiple placements — only invalid ones reported
	// -----------------------------------------------------------------------

	/**
	 * With threshold=75%: box B (50% supported) fails, box C (100% supported) passes.
	 */
	@Test
	void testMixedPlacements_onlyInvalidReported() {
		PercentSupportedStabilityValidator validator = new PercentSupportedStabilityValidator(75);

		Placement supA = makePlacement("SA",  5, 10, 1, 0,  0, 0);
		Placement b    = makePlacement("B",  10, 10, 1, 0,  0, 1);
		Placement supC = makePlacement("SC", 10, 10, 1, 0, 20, 0);
		Placement c    = makePlacement("C",  10, 10, 1, 0, 20, 1);

		supA.addLoad(b, 50L, b.getWeight());   // B: 50% < 75 → invalid
		supC.addLoad(c, 100L, c.getWeight());  // C: 100% ≥ 75 → valid

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(supA, b, supC, c), reasons)).isFalse();
		assertThat(reasons).hasSize(1);
		assertThat(((InsufficientSupportAreaReason) reasons.get(0)).getPlacement()).isSameAs(b);
	}
}
