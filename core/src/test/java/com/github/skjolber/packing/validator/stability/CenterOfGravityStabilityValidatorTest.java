package com.github.skjolber.packing.validator.stability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.reasons.UnstableStackCenterOfGravityReason;

/**
 * Unit tests for {@link CenterOfGravityStabilityValidator}.
 *
 * <p>This validator checks the effective centre of gravity of the entire vertical
 * stack (this box plus all boxes above) against the direct supporters.
 * The key difference from {@link CenterOfGravitySupportStabilityValidator} is that
 * a heavy overhanging box on top can shift the combined CoG outside the support
 * region even when the box's own CoG is within bounds.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class CenterOfGravityStabilityValidatorTest {

	private final CenterOfGravityStabilityValidator validator =
			new CenterOfGravityStabilityValidator();

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
	 * A placement on the container floor has no supporters but is always considered stable.
	 */
	@Test
	void testFloorPlacement_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 10, 0, 0, 0);

		List<Placement> placements = List.of(a);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Elevated with no supporters: unstable
	// -----------------------------------------------------------------------

	/**
	 * An elevated placement with no supporters is always unstable.
	 */
	@Test
	void testElevatedNoSupporters_invalid() {
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		List<Placement> placements = List.of(b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		UnstableStackCenterOfGravityReason reason = (UnstableStackCenterOfGravityReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(b);
		assertThat(reason.getCode()).isEqualTo(22);
	}

	// -----------------------------------------------------------------------
	// Fully supported (fast path)
	// -----------------------------------------------------------------------

	/**
	 * Box B is fully supported by A — always stable regardless of stack weight.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  fully supported → valid
	 *  1  +----------+
	 *     |    A     |
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testFullySupported_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Stack CoG outside support: unstable even when box's own CoG is within
	// -----------------------------------------------------------------------

	/**
	 * Box B (6×10) sits on A (5×10) with its left edge aligned.
	 * B overhangs A on the right (1 unit), but its own CoG (x=4) is within support [1,4].
	 * Box C (3×10, very heavy) is placed at the far right end of B, shifting the combined
	 * CoG of B+C well outside A's support region.
	 *
	 * <pre>
	 *  z (side view, Y ignored)
	 *  |
	 *  3       +---+
	 *           | C |  weight=1000, x=5..7
	 *  2  +------+
	 *     |  B   |   weight=1, x=1..6; own CoG at x=4, support=[1,4] → within
	 *  1  +-----+
	 *     |  A  |    weight=20, x=0..4
	 *  0  +-----+
	 *     0  1  4 5  7  x
	 * </pre>
	 *
	 * B.supportedArea = overlap(B[1..6], A[0..4]) = 4×10 = 40 ≠ B.area=60 (not fast-path).
	 * B own CoG: com2x = 2*1+6=8. Support x=[1,4]: 2≤8≤8 → within ✓
	 * Stack CoG of B+C: dominated by C (weight 1000) at x≈6.5 → com2x≈13 > 8=2*maxSupportX → outside ✗
	 */
	@Test
	void testSupportCoGInside_stackCoGOutside_invalid() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  6, 10, 1,  1, 1, 0, 1);
		Placement c = makePlacement("C",  3, 10, 1, 1000, 5, 0, 2);

		// A is the sole supporter of B; overlap area = 4×10=40, A bears all of B's weight
		a.addLoad(b, 40L, b.getWeight());
		// B is the sole supporter of C; overlap area = (6-5+1)*10=20, B bears all of C's weight
		b.addLoad(c, 20L, c.getWeight());

		List<Placement> placements = List.of(a, b, c);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		// B's own CoG is within A's support → CenterOfGravitySupportStabilityValidator passes B
		boolean bOwnStable = CenterOfGravitySupportStabilityValidator.isPlacementStableSupport(b);
		assertThat(bOwnStable).isTrue();

		// B's stack CoG (combined with heavy C) is outside A's support → fails
		assertThat(validator.isStable(placements, reasons)).isFalse();

		boolean bFound = reasons.stream()
				.map(r -> ((UnstableStackCenterOfGravityReason) r).getPlacement())
				.anyMatch(p -> p == b);
		assertThat(bFound).isTrue();
	}

	// -----------------------------------------------------------------------
	// Stack CoG within support: stable
	// -----------------------------------------------------------------------

	/**
	 * Simple three-level symmetric stack: A → B → C, all same 10×10 footprint.
	 * All boxes are fully supported (fast path). All placements stable.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |
	 *  2  +----------+
	 *     |    B     |
	 *  1  +----------+
	 *     |    A     |
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testSymmetricStack_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1,  5, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<Placement> placements = List.of(a, b, c);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}
}
