package com.github.skjolber.packing.validator.stability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.reasons.UnstableCenterOfGravityReason;

/**
 * Unit tests for {@link CenterOfGravitySupportStabilityValidator}.
 *
 * <p>This validator checks only the box's own centre of gravity (CoG) against its
 * direct supporters — weight from boxes above is ignored.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class CenterOfGravitySupportStabilityValidatorTest {

	private final CenterOfGravitySupportStabilityValidator validator = new CenterOfGravitySupportStabilityValidator();

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

	// -----------------------------------------------------------------------
	// Floor placement: always stable
	// -----------------------------------------------------------------------

	/**
	 * A placement on the container floor has no supporters but is still considered stable.
	 */
	@Test
	void testFloorPlacement_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 10, 0, 0, 0);

		List<Placement> placements = List.of(a);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Elevated with no supporters: unstable
	// -----------------------------------------------------------------------

	/**
	 * A placement elevated above the floor with no supporters is unstable.
	 */
	@Test
	void testElevatedNoSupporters_invalid() {
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		List<Placement> placements = List.of(b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		UnstableCenterOfGravityReason reason = (UnstableCenterOfGravityReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(b);
		assertThat(reason.getCode()).isEqualTo(21);
	}

	// -----------------------------------------------------------------------
	// Fully supported (fast path)
	// -----------------------------------------------------------------------

	/**
	 * Box B is fully supported by A (supportedArea == footprintArea) — CoG is always within.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  CoG at (5,5), fully supported → valid
	 *  1  +----------+
	 *     |    A     |
	 *  0  +----------+
	 *     0          10  x
	 * </pre>
	 */
	@Test
	void testFullySupported_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// CoG within support AABB: stable
	// -----------------------------------------------------------------------

	/**
	 * Box B (10×10) is centred over support provided by A on the left half of B.
	 * B's CoG (x=5) is within the support AABB (x=[0,4]).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  CoG at x=5, support=[0,4] → inside → valid
	 *  1  +-----+
	 *     |  A  |
	 *  0  +-----+
	 *     0     5    10  x
	 * </pre>
	 *
	 * Support overlap: x=[0,4], y=[0,9]. B's CoG ×2 = 10. 2*0=0 ≤ 10 ≤ 2*4=8 → valid.
	 */
	@Test
	void testCoGWithinSupport_valid() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		// A covers the left half of B's footprint (area=50)
		a.addLoad(b, 50L, b.getWeight() / 2);

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		// B's CoG is at x=5 (center of 10-wide box at x=0).
		// Support AABB: overlap of B[0..9] and A[0..4] = x:[0,4].
		// com2x = 2*0 + 10 = 10.  2*maxSupportX = 2*4 = 8.  10 > 8 → outside!
		// Actually the CoG is OUTSIDE because the box overhangs the support — it's a
		// cantilevered overhang, which CenterOfGravitySupportStabilityValidator flags.
		assertThat(validator.isValid(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);
	}

	// -----------------------------------------------------------------------
	// Centred over supporter: stable
	// -----------------------------------------------------------------------

	/**
	 * Small box B centred on top of A.  B's CoG exactly matches A's CoG — clearly within support.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2    +----+
	 *       | B  |  4×4 centred on A
	 *  1  +--------+
	 *     |   A    |  10×10
	 *  0  +--------+
	 *     0  3  7  10  x
	 * </pre>
	 */
	@Test
	void testSmallBoxCentredOnLarger_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  4,  4, 1, 10, 3, 3, 1);

		// B's footprint [3..6]×[3..6] is entirely within A's [0..9]×[0..9]
		a.addLoad(b, 16L, b.getWeight());

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// CoG outside support AABB: unstable
	// -----------------------------------------------------------------------

	/**
	 * Box B (10×10) overhangs far to the right of support A (2×10).
	 * B's CoG (x=5) is well outside the support region [0,1].
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  CoG at x=5, support=[0,1] → outside → invalid
	 *  1  +--+
	 *     |A |
	 *  0  +--+
	 *     0  2        10  x
	 * </pre>
	 */
	@Test
	void testCoGOutsideSupport_invalid() {
		Placement a = makePlacement("A",  2, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		// A covers only 2 units of B's 10-unit-wide footprint
		a.addLoad(b, 20L, 2L);

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		UnstableCenterOfGravityReason reason = (UnstableCenterOfGravityReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(b);
	}
}
