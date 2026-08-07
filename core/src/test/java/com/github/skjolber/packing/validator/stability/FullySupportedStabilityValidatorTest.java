package com.github.skjolber.packing.validator.stability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * Unit tests for {@link FullySupportedStabilityValidator}.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class FullySupportedStabilityValidatorTest {

	private final FullySupportedStabilityValidator validator = new FullySupportedStabilityValidator();

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
	// Floor placements (z == 0) are always valid regardless of support area
	// -----------------------------------------------------------------------

	/**
	 * A placement on the container floor ({@code z == 0}) with no supporters is always valid.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  1  +----------+
	 *     |    A     |  z=0, no supporters → valid
	 *  0  +----------+
	 *     0          10  x
	 * </pre>
	 */
	@Test
	void testFloorPlacement_noSupporters_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 0, 0, 0);

		List<Placement> placements = List.of(a);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Elevated, fully supported
	// -----------------------------------------------------------------------

	/**
	 * Box B is elevated and fully supported by A (supportedArea == footprintArea).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  supportedArea=100 == area=100 → valid
	 *  1  +----------+
	 *     |    A     |
	 *  0  +----------+
	 *     0          10  x
	 * </pre>
	 */
	@Test
	void testElevatedFullySupported_valid() {
		Placement a = makePlacement("A", 10, 10, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Elevated, partially supported → invalid
	// -----------------------------------------------------------------------

	/**
	 * Box B (10×10) is elevated and only half-supported by A (5×10 overlap).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  supportedArea=50 < area=100 → invalid
	 *  1  +-----+
	 *     |  A  |
	 *  0  +-----+
	 *     0     5   10  x
	 * </pre>
	 */
	@Test
	void testElevatedPartiallySupported_invalid() {
		Placement a = makePlacement("A",  5, 10, 1, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);

		a.addLoad(b, 50L, b.getWeight() / 2);

		List<Placement> placements = List.of(a, b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		InsufficientSupportAreaReason reason = (InsufficientSupportAreaReason) reasons.get(0);
		assertThat(reason.getPlacement()).isSameAs(b);
		assertThat(reason.getSupportedArea()).isEqualTo(50L);
		assertThat(reason.getRequiredArea()).isEqualTo(100L);
		assertThat(reason.getCode()).isEqualTo(20);
	}

	// -----------------------------------------------------------------------
	// Elevated with no supporters → invalid
	// -----------------------------------------------------------------------

	/**
	 * Box B is elevated ({@code z > 0}) but has no supporters at all.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  no supporters, z=1 → invalid
	 *  1
	 *  0
	 *     0          10  x
	 * </pre>
	 */
	@Test
	void testElevatedNoSupporters_invalid() {
		Placement b = makePlacement("B", 10, 10, 1, 0, 0, 1);

		List<Placement> placements = List.of(b);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);

		InsufficientSupportAreaReason reason = (InsufficientSupportAreaReason) reasons.get(0);
		assertThat(reason.getSupportedArea()).isEqualTo(0L);
		assertThat(reason.getRequiredArea()).isEqualTo(100L);
	}

	// -----------------------------------------------------------------------
	// Multiple placements — only invalid ones are reported
	// -----------------------------------------------------------------------

	/**
	 * Mix of floor, fully-supported, and partially-supported placements.
	 * Only the partially-supported one should produce a reason.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+  +---+
	 *     |    B     |  | C |  B: fully supported, C: partially → invalid
	 *  1  +----------+  |   |
	 *     |    A     |  +---+
	 *  0  +----------+
	 *     0          10  15  x
	 * </pre>
	 */
	@Test
	void testMixedPlacements_onlyInvalidReported() {
		Placement a  = makePlacement("A", 10, 10, 1, 0, 0, 0);
		Placement b  = makePlacement("B", 10, 10, 1, 0, 0, 1);
		Placement sup = makePlacement("S",  5, 10, 1, 10, 0, 0);
		Placement c  = makePlacement("C", 10, 10, 1, 10, 0, 1);

		a.addLoad(b, 100L, b.getWeight());     // B fully supported
		sup.addLoad(c, 50L, c.getWeight() / 2); // C only half-supported

		List<Placement> placements = List.of(a, b, sup, c);
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(validator.isStable(placements, reasons)).isFalse();
		assertThat(reasons).hasSize(1);
		assertThat(((InsufficientSupportAreaReason) reasons.get(0)).getPlacement()).isSameAs(c);
	}
}
