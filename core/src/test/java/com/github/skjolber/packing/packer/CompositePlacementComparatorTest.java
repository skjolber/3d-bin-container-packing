package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.CompositePlacementComparator;
import com.github.skjolber.packing.comparator.PlacementComparator;
import com.github.skjolber.packing.comparator.PlacementComparatorBuilder;

/**
 * Unit tests for {@link CompositePlacementComparator}.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 */
class CompositePlacementComparatorTest {

	// =========================================================================
	// Basic delegation
	// =========================================================================

	/**
	 * First comparator is decisive when it returns non-zero.
	 *
	 * <pre>
	 *  Composite: constraint (noIdentical) → position (lowerZ)
	 *
	 *  A: no restriction, z=5    B: identical-only, z=0
	 *
	 *  Constraint fires first: A (unrestricted) beats B even though B is lower.
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void firstComparatorIsDecisive() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter().build();
		PlacementComparator position = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter().build();
		PlacementComparator composite = new CompositePlacementComparator(constraint, position);

		Placement unrestricted = placement(5, 5, 1, 0, 0, 5, 1, false, -1, -1, -1);
		Placement identOnly    = placement(5, 5, 1, 0, 0, 0, 1, true,  -1, -1, -1);

		assertThat(composite.compare(unrestricted, identOnly)).isPositive();
	}

	/**
	 * Second comparator decides when the first returns zero.
	 *
	 * <pre>
	 *  Composite: constraint (noIdentical) → position (lowerZ)
	 *
	 *  Both unrestricted; constraint tie → position breaks it.
	 *
	 *  A: unrestricted, z=1    B: unrestricted, z=5
	 *
	 *  Constraint: tie (both unrestricted)
	 *  Position: A wins (z=1 < z=5)
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void secondComparatorBreaksTie() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter().build();
		PlacementComparator position = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter().build();
		PlacementComparator composite = new CompositePlacementComparator(constraint, position);

		Placement lowUnrestricted  = p(5, 5, 1, 0, 0, 1, 1);
		Placement highUnrestricted = p(5, 5, 1, 0, 0, 5, 1);

		assertThat(composite.compare(lowUnrestricted, highUnrestricted)).isPositive();
	}

	/**
	 * Both comparators return zero → composite returns zero.
	 *
	 * <pre>
	 *  A and B: both unrestricted, same z → first and second both 0.
	 *
	 *  Expected: compare(A, B) = 0
	 * </pre>
	 */
	@Test
	void bothZero_returnsZero() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter().build();
		PlacementComparator position = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter().build();
		PlacementComparator composite = new CompositePlacementComparator(constraint, position);

		Placement a = p(5, 5, 1, 0, 0, 2, 1);
		Placement b = p(5, 5, 1, 0, 0, 2, 1);
		assertThat(composite.compare(a, b)).isZero();
	}

	// =========================================================================
	// thenComparing convenience method
	// =========================================================================

	/**
	 * {@code thenComparing} produces a {@link CompositePlacementComparator}.
	 *
	 * <pre>
	 *  constraint.thenComparing(position) ≡ new CompositePlacementComparator(constraint, position)
	 * </pre>
	 */
	@Test
	void thenComparing_producesComposite() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.higherMaxLoadWeightIsBetter().build();
		PlacementComparator position = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter().build();

		PlacementComparator combined = constraint.thenComparing(position);

		assertThat(combined).isInstanceOf(CompositePlacementComparator.class);
		assertThat(((CompositePlacementComparator) combined).getFirst()).isSameAs(constraint);
		assertThat(((CompositePlacementComparator) combined).getSecond()).isSameAs(position);
	}

	/**
	 * Full round-trip: constraint chain → position chain combined via {@code thenComparing}.
	 *
	 * <pre>
	 *  Combined priority: noIdentical → higherMaxLoadWeight → lowerZ → higherArea
	 *
	 *  Step 1 — identical flag decides:
	 *
	 *    A: unrestricted, weight=50, z=5, area=100
	 *    B: identical-only, weight=500, z=0, area=900
	 *    → A wins (no restriction beats identical-only)
	 *
	 *  Step 2 — when identical flags equal, maxLoadWeight decides:
	 *
	 *    A: unrestricted, weight=500, z=5, area=4
	 *    B: unrestricted, weight=100, z=0, area=900
	 *    → A wins (higher weight limit)
	 *
	 *  Step 3 — when constraint tie, z decides:
	 *
	 *    A: unrestricted, weight=100, z=1, area=4
	 *    B: unrestricted, weight=100, z=5, area=900
	 *    → A wins (lower z)
	 *
	 *  Step 4 — when z also equal, area decides:
	 *
	 *    A: unrestricted, weight=100, z=2, area=900
	 *    B: unrestricted, weight=100, z=2, area=4
	 *    → A wins (larger area)
	 * </pre>
	 */
	@Test
	void fullRoundTrip_identicalDecides() {
		PlacementComparator cmp = combined();
		assertThat(cmp.compare(
				placement(10, 10, 1, 0, 0, 5, 1, false, -1, 50,  -1),
				placement(30, 30, 1, 0, 0, 0, 1, true,  -1, 500, -1))).isPositive();
	}

	@Test
	void fullRoundTrip_weightDecides() {
		PlacementComparator cmp = combined();
		assertThat(cmp.compare(
				placement(2, 2, 1, 0, 0, 5, 1, false, -1, 500, -1),
				placement(30, 30, 1, 0, 0, 0, 1, false, -1, 100, -1))).isPositive();
	}

	@Test
	void fullRoundTrip_zDecides() {
		PlacementComparator cmp = combined();
		assertThat(cmp.compare(
				placement(2, 2, 1, 0, 0, 1, 1, false, -1, 100, -1),
				placement(30, 30, 1, 0, 0, 5, 1, false, -1, 100, -1))).isPositive();
	}

	@Test
	void fullRoundTrip_areaDecides() {
		PlacementComparator cmp = combined();
		assertThat(cmp.compare(
				placement(30, 30, 1, 0, 0, 2, 1, false, -1, 100, -1),
				placement(2,  2,  1, 0, 0, 2, 1, false, -1, 100, -1))).isPositive();
	}

	// =========================================================================
	// Null handling
	// =========================================================================

	@Test
	void nullFirst_throwsNullPointerException() {
		PlacementComparator position = PlacementComparatorBuilder.newBuilder().lowerZIsBetter().build();
		assertThatThrownBy(() -> new CompositePlacementComparator(null, position))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void nullSecond_throwsNullPointerException() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter().build();
		assertThatThrownBy(() -> new CompositePlacementComparator(constraint, null))
				.isInstanceOf(NullPointerException.class);
	}

	// =========================================================================
	// Deep nesting
	// =========================================================================

	@Test
	void deeplyNested_compositeOfComposites() {
		PlacementComparator c1 = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter().build();
		PlacementComparator c2 = PlacementComparatorBuilder.newBuilder()
				.higherMaxLoadWeightIsBetter().build();
		PlacementComparator c3 = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter().build();

		PlacementComparator nested = c1.thenComparing(c2).thenComparing(c3);

		// identical decides
		assertThat(nested.compare(p(5, 5, 1, 0, 0, 5, 1), pIdentOnly(5, 5, 1, 0, 0, 0))).isPositive();
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	/** Constraint: noIdentical → higherMaxLoadWeight;  Position: lowerZ → higherArea. */
	private static PlacementComparator combined() {
		PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadWeightIsBetter()
				.build();
		PlacementComparator position = PlacementComparatorBuilder.newBuilder()
				.lowerZIsBetter()
				.higherAreaIsBetter()
				.build();
		return constraint.thenComparing(position);
	}

	private static Placement p(int dx, int dy, int dz, int x, int y, int z, int weight) {
		Box box = Box.newBuilder()
				.withId("b-" + dx + "x" + dy + "x" + dz + "w" + weight)
				.withSize(dx, dy, dz).withWeight(weight).build();
		new BoxItem(box);
		Placement pl = new Placement(box.getStackValue(0), 0, x, y, z);
		pl.setSupportedArea(box.getStackValue(0).getArea());
		return pl;
	}

	private static Placement pIdentOnly(int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId("b-identical-" + dx + "x" + dy + "x" + dz)
				.withSize(dx, dy, dz).withWeight(1)
				.withMaxLoadIdenticalBoxCount(-1).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, x, y, z);
	}

	/**
	 * General-purpose placement factory.
	 *
	 * @param maxCount    pass -1 for unconstrained count
	 * @param maxWeight   pass -1 for unconstrained weight
	 * @param maxPressure pass -1 for unconstrained pressure
	 */
	private static Placement placement(int dx, int dy, int dz, int x, int y, int z, int weight,
			boolean identicalOnly, int maxCount, long maxWeight, double maxPressure) {
		Box.Builder builder = Box.newBuilder()
				.withId("b-complex")
				.withSize(dx, dy, dz).withWeight(weight);
		if (maxWeight > 0)   builder.withMaxLoadWeight(maxWeight);
		if (maxPressure > 0) builder.withMaxLoadPressure(maxPressure);
		if (identicalOnly) {
			builder.withMaxLoadIdenticalBoxCount(maxCount > 0 ? maxCount : -1);
		} else if (maxCount > 0) {
			builder.withMaxLoadBoxCount(maxCount);
		}
		Box box = builder.build();
		new BoxItem(box);
		Placement pl = new Placement(box.getStackValue(0), 0, x, y, z);
		pl.setSupportedArea(box.getStackValue(0).getArea());
		return pl;
	}
}
