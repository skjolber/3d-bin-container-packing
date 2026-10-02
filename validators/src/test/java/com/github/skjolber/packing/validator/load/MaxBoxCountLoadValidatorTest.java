package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;

/**
 * Unit tests for {@link MaxBoxCountLoadValidator}.
 *
 * <p>Box count is measured as the depth of the supportee subtree above each constrained
 * placement (0 = nothing on top, 1 = one box directly on top, etc.).
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class MaxBoxCountLoadValidatorTest {

	private final MaxBoxCountLoadValidator validator = new MaxBoxCountLoadValidator();

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

	private static Placement makePlacementWithBoxCountLimit(String id, int dx, int dy, int dz,
			int weight, int maxBoxCount, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withMaxLoadBoxCount(maxBoxCount)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// No constraint: always valid
	// -----------------------------------------------------------------------

	@Test
	void testNoConstraint_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 10, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 2);
		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Nothing on top of a constrained placement: valid
	// -----------------------------------------------------------------------

	/**
	 * A has maxLoadBoxCount=2 but nothing rests on it → depth=0 → valid.
	 */
	@Test
	void testConstrainedNothingOnTop_valid() {
		Placement a = makePlacementWithBoxCountLimit("A", 10, 10, 1, 10, 2, 0, 0, 0);

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Within the limit
	// -----------------------------------------------------------------------

	/**
	 * A allows 2 levels above it. B and C are stacked on top → depth=2 ≤ 2 → valid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  level 2 above A
	 *  2  +----------+
	 *     |    B     |  level 1 above A
	 *  1  +----------+
	 *     |    A     |  maxLoadBoxCount=2
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testAtLimit_valid() {
		Placement a = makePlacementWithBoxCountLimit("A", 10, 10, 1, 30, 2, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Exceeds the limit
	// -----------------------------------------------------------------------

	/**
	 * A allows only 1 level above it. B and C are stacked on top → depth=2 > 1 → invalid.
	 */
	@Test
	void testExceedsLimit_invalid() {
		Placement a = makePlacementWithBoxCountLimit("A", 10, 10, 1, 30, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c), reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		ExcessiveLoadBoxCountReason reason = (ExcessiveLoadBoxCountReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(a);
		assertThat(reason.getBoxCount()).isEqualTo(2);
		assertThat(reason.getMaxLoadBoxCount()).isEqualTo(1);
		assertThat(reason.getCode()).isEqualTo(12);
	}

	// -----------------------------------------------------------------------
	// Limit of 0: nothing may be placed on top
	// -----------------------------------------------------------------------

	/**
	 * A allows 0 levels above. B is placed on top → depth=1 > 0 → invalid.
	 */
	@Test
	void testZeroLimit_oneBoxOnTop_invalid() {
		Placement a = makePlacementWithBoxCountLimit("A", 10, 10, 1, 20, 0, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 5, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b), reasons)).isFalse();
		assertThat(reasons).hasSize(1);
		assertThat(((ExcessiveLoadBoxCountReason) reasons.get(0)).getBoxCount()).isEqualTo(1);
	}

	// -----------------------------------------------------------------------
	// Branching stack: depth measured on longest branch
	// -----------------------------------------------------------------------

	/**
	 * A (maxCount=1) supports both B and C directly.  B then supports D.
	 * The longest branch above A is A→B→D = depth 2 > 1 → invalid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +-----+
	 *     |  D  |
	 *  2  +-----+  +-----+
	 *     |  B  |  |  C  |    depth via B branch = 2, via C branch = 1
	 *  1  +-----------+
	 *     |      A    |  maxLoadBoxCount=1
	 *  0  +-----------+
	 * </pre>
	 */
	@Test
	void testBranchingStack_longestBranchExceedsLimit_invalid() {
		Placement a = makePlacementWithBoxCountLimit("A", 10, 10, 1, 50, 1, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C",  5, 10, 1, 10, 5, 0, 1);
		Placement d = makePlacement("D",  5, 10, 1,  5, 0, 0, 2);

		a.addLoad(b, 50L, b.getWeight());
		a.addLoad(c, 50L, c.getWeight());
		b.addLoad(d, 50L, d.getWeight());

		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isValid(List.of(a, b, c, d), reasons)).isFalse();
		ExcessiveLoadBoxCountReason reason = (ExcessiveLoadBoxCountReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(a);
		assertThat(reason.getBoxCount()).isEqualTo(2);
	}
}
