package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;

/**
 * Unit tests for {@link DefaultLoadValidatorBuilder} and {@link CompositeLoadValidator}.
 *
 * <p>Coordinate system: X = width, Y = depth, Z = height.
 */
public class DefaultLoadValidatorBuilderTest {

	private static final Container CONTAINER = Container.newBuilder()
			.withId("C")
			.withSize(100, 100, 100)
			.withEmptyWeight(0)
			.withMaxLoadWeight(10000)
			.withStack(new Stack())
			.build();

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static Placement placement(String id, int dx, int dy, int dz, int weight, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withRotate2D()
				.build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	private static Placement placementWithWeightLimit(String id, int dx, int dy, int dz,
			int weight, long maxLoadWeight, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadWeight(maxLoadWeight).withRotate2D().build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	private static Placement placementWithPressureLimit(String id, int dx, int dy, int dz,
			int weight, double maxPressure, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadPressure(maxPressure).withRotate2D().build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	private static Placement placementWithBoxCountLimit(String id, int dx, int dy, int dz,
			int weight, int maxBoxCount, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadBoxCount(maxBoxCount).withRotate2D().build();
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	private static Placement placementIdenticalOnly(String id, int dx, int dy, int dz, int weight,
			int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadIdenticalBoxCount(-1).withRotate2D().build();
		new BoxItem(box); // registers BoxItem on the box so getBoxItem() is non-null
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// Guard: placements not set
	// -----------------------------------------------------------------------

	@Test
	void testBuild_withoutPlacements_throws() {
		assertThatThrownBy(() -> new DefaultLoadValidatorBuilder()
				.withContainer(CONTAINER)
				.build())
				.isInstanceOf(IllegalStateException.class);
	}

	// -----------------------------------------------------------------------
	// No constraints → null returned
	// -----------------------------------------------------------------------

	/**
	 * When no placement has any load constraint, no validator is needed — {@code null} is returned.
	 */
	@Test
	void testNoConstraints_returnsNull() {
		List<Placement> placements = List.of(placement("A", 10, 10, 1, 5, 0, 0, 0));

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(placements)
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isNull();
	}

	// -----------------------------------------------------------------------
	// Single constraint type → validator returned directly (not wrapped)
	// -----------------------------------------------------------------------

	@Test
	void testOnlyWeightConstraint_returnsWeightValidator() {
		Placement a = placementWithWeightLimit("A", 10, 10, 1, 5, 100L, 0, 0, 0);

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a))
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(WeightLoadValidator.class);
	}

	@Test
	void testOnlyPressureConstraint_returnsPressureValidator() {
		Placement a = placementWithPressureLimit("A", 10, 10, 1, 5, 500.0, 0, 0, 0);

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a))
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(MaxPressureLoadValidator.class);
	}

	@Test
	void testOnlyBoxCountConstraint_returnsBoxCountValidator() {
		Placement a = placementWithBoxCountLimit("A", 10, 10, 1, 5, 3, 0, 0, 0);

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a))
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(MaxBoxCountLoadValidator.class);
	}

	@Test
	void testOnlyIdenticalConstraint_returnsIdenticalValidator() {
		Placement a = placementIdenticalOnly("A", 10, 10, 1, 5, 0, 0, 0);

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a))
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(IdenticalBoxOnlyLoadValidator.class);
	}

	// -----------------------------------------------------------------------
	// Multiple constraint types → CompositeLoadValidator
	// -----------------------------------------------------------------------

	/**
	 * When both weight and box-count constraints are present, a
	 * {@link CompositeLoadValidator} containing both is returned.
	 */
	@Test
	void testWeightAndBoxCount_returnsComposite() {
		Placement a = placementWithWeightLimit("A", 10, 10, 1, 5, 50L, 0, 0, 0);
		Placement b = placementWithBoxCountLimit("B", 10, 10, 1, 5, 2,  0, 0, 1);

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a, b))
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(CompositeLoadValidator.class);
		CompositeLoadValidator composite = (CompositeLoadValidator) result;
		assertThat(composite.getValidators()).hasSize(2);
		assertThat(composite.getValidators().get(0)).isInstanceOf(WeightLoadValidator.class);
		assertThat(composite.getValidators().get(1)).isInstanceOf(MaxBoxCountLoadValidator.class);
	}

	/**
	 * All four constraint types present → composite with four validators, in canonical order:
	 * weight, pressure, box-count, identical.
	 */
	@Test
	void testAllFourConstraints_returnsCompositeWithFour() {
		List<Placement> placements = List.of(
				placementWithWeightLimit("W",   10, 10, 1, 5, 100L,  0,  0, 0),
				placementWithPressureLimit("P",  10, 10, 1, 5, 500.0, 10,  0, 0),
				placementWithBoxCountLimit("C",  10, 10, 1, 5, 2,    20,  0, 0),
				placementIdenticalOnly("I",      10, 10, 1, 5,       30,  0, 0));

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(placements)
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(CompositeLoadValidator.class);
		List<LoadValidator> delegates = ((CompositeLoadValidator) result).getValidators();
		assertThat(delegates).hasSize(4);
		assertThat(delegates.get(0)).isInstanceOf(WeightLoadValidator.class);
		assertThat(delegates.get(1)).isInstanceOf(MaxPressureLoadValidator.class);
		assertThat(delegates.get(2)).isInstanceOf(MaxBoxCountLoadValidator.class);
		assertThat(delegates.get(3)).isInstanceOf(IdenticalBoxOnlyLoadValidator.class);
	}

	// -----------------------------------------------------------------------
	// CompositeLoadValidator: all violations collected in one pass
	// -----------------------------------------------------------------------

	/**
	 * Both weight and box-count constraints are violated.
	 * The composite must collect both reasons rather than stopping at the first failure.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  weight=30 (makes B exceed weight limit)
	 *  2  +----------+
	 *     |    B     |  weight=10 (makes A exceed weight limit too)
	 *  1  +----------+
	 *     |    A     |  maxLoadWeight=5, maxLoadBoxCount=1
	 *  0  +----------+
	 * </pre>
	 */
	@Test
	void testComposite_collectsAllViolations() {
		Box boxA = Box.newBuilder()
				.withId("A").withSize(10, 10, 1).withWeight(5)
				.withMaxLoadWeight(5L).withMaxLoadBoxCount(1)
				.withRotate2D().build();
		Placement a = new Placement(boxA.getStackValues()[0], 0, 0, 0, 0);
		Placement b = placement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = placement("C", 10, 10, 1, 30, 0, 0, 2);

		a.addLoad(b, 100L, b.getWeight());
		b.addLoad(c, 100L, c.getWeight());

		LoadValidator validator = new DefaultLoadValidatorBuilder()
				.withPlacements(List.of(a, b, c))
				.withContainer(CONTAINER)
				.build();

		assertThat(validator).isInstanceOf(CompositeLoadValidator.class);

		List<ValidatorResultReason> reasons = new ArrayList<>();
		boolean valid = validator.isValid(List.of(a, b, c), reasons);

		assertThat(valid).isFalse();
		// Both weight violation (loadWeight=40 > 5) and box-count violation (depth=2 > 1)
		assertThat(reasons).hasSize(2);
		assertThat(reasons).anyMatch(r -> r instanceof ExcessiveLoadWeightReason);
		assertThat(reasons).anyMatch(r -> r instanceof ExcessiveLoadBoxCountReason);
	}

	// -----------------------------------------------------------------------
	// CompositeLoadValidator: empty delegates list always returns true
	// -----------------------------------------------------------------------

	@Test
	void testComposite_empty_alwaysValid() {
		CompositeLoadValidator composite = new CompositeLoadValidator(List.of());
		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(composite.isValid(List.of(), reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Constraint detected only on one placement among unconstrained ones
	// -----------------------------------------------------------------------

	/**
	 * A mix of constrained and unconstrained placements: the builder detects
	 * the weight constraint from the single constrained placement and returns
	 * the appropriate validator.
	 */
	@Test
	void testMixedPlacements_constraintDetectedFromAny() {
		List<Placement> placements = List.of(
				placement("A", 10, 10, 1, 5, 0, 0, 0),               // no constraint
				placementWithWeightLimit("B", 10, 10, 1, 5, 20L, 0, 0, 1), // weight constraint
				placement("C", 10, 10, 1, 5, 0, 0, 2));               // no constraint

		LoadValidator result = new DefaultLoadValidatorBuilder()
				.withPlacements(placements)
				.withContainer(CONTAINER)
				.build();

		assertThat(result).isInstanceOf(WeightLoadValidator.class);
	}
}
