package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.AbstractChainedPlacementComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorAttribute;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorFactory;

/**
 * Unit tests for {@link PlacementComparatorFactory}.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 */
class PlacementComparatorBuilderTest {

	// =========================================================================
	// emptyBuilder / newBuilder semantics
	// =========================================================================

	/**
	 * {@code emptyBuilder()} has no entries — produces a no-op comparator.
	 *
	 * <pre>
	 *  emptyBuilder().build() → always returns 0
	 * </pre>
	 */
	@Test
	void emptyBuilder_alwaysReturnsZero() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), p(5, 5, 1, 3, 3, 3, 10))).isZero();
	}

	/**
	 * {@code newBuilder()} pre-loads all four constraint entries in natural priority order.
	 * Calling {@code build()} immediately produces the fused all-constraint comparator.
	 *
	 * <pre>
	 *  newBuilder().build()
	 *  → NoIdenticalHigherCountHigherWeightHigherPressureComparator
	 *
	 *  A: unrestricted  B: identical-only  → A preferred
	 * </pre>
	 */
	@Test
	void newBuilder_preloadsAllFourConstraintEntries_identicalIsFirst() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().build();
		assertThat(cmp).isInstanceOf(
				DefaultPlacementComparatorFactory.NoIdenticalHigherCountHigherWeightHigherPressureComparator.class);
		assertThat(cmp.compare(pAllConstraints(false, 5, 500, 9.0),
				pAllConstraints(true, 2, 100, 1.0))).isPositive();
	}

	// =========================================================================
	// Position — Z coordinate
	// =========================================================================

	/**
	 * {@code lowerZIsBetter()} prefers placements closer to the container floor.
	 *
	 * <pre>
	 *  Side view:
	 *
	 *   z=5  ┌──────┐  ← B  (elevated, less preferred)
	 *   z=1  ┌──────┐  ← A  (near floor, preferred)
	 *   z=0  ════════  floor
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void lowerZIsBetter_lowerZWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerZIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 5, 1))).isPositive();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 5, 1), p(5, 5, 1, 0, 0, 1, 1))).isNegative();
	}

	@Test
	void higherZIsBetter_higherZWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherZIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 5, 1), p(5, 5, 1, 0, 0, 1, 1))).isPositive();
	}

	// =========================================================================
	// Position — X / Y coordinates
	// =========================================================================

	/**
	 * {@code lowerXIsBetter()} prefers placements closer to x=0.
	 *
	 * <pre>
	 *  Top view:
	 *
	 *  y
	 *  │  ┌─┐ A(x=0)          ┌─┐ B(x=8)
	 *  └──┴─┴─────────────────┴─┴── x
	 *     0                   8
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void lowerXIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerXIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 1, 0, 0, 0, 1), p(2, 2, 1, 8, 0, 0, 1))).isPositive();
	}

	@Test
	void higherXIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherXIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 1, 8, 0, 0, 1), p(2, 2, 1, 0, 0, 0, 1))).isPositive();
	}

	@Test
	void lowerYIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerYIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 1, 0, 0, 0, 1), p(2, 2, 1, 0, 8, 0, 1))).isPositive();
	}

	@Test
	void higherYIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherYIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 1, 0, 8, 0, 1), p(2, 2, 1, 0, 0, 0, 1))).isPositive();
	}

	// =========================================================================
	// Position — area
	// =========================================================================

	/**
	 * {@code higherAreaIsBetter()} prefers the larger footprint.
	 *
	 * <pre>
	 *  Overhead view:
	 *
	 *  A: 3×3 = area 9      B: 6×6 = area 36
	 *  ┌───┐                ┌──────┐
	 *  │ A │                │  B   │  ← preferred (larger footprint)
	 *  └───┘                └──────┘
	 *
	 *  Expected: compare(B, A) > 0
	 * </pre>
	 */
	@Test
	void higherAreaIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherAreaIsBetter().build();
		assertThat(cmp.compare(p(6, 6, 2, 0, 0, 0, 1), p(3, 3, 2, 0, 0, 0, 1))).isPositive();
		assertThat(cmp.compare(p(3, 3, 2, 0, 0, 0, 1), p(6, 6, 2, 0, 0, 0, 1))).isNegative();
	}

	@Test
	void lowerAreaIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerAreaIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 1, 0, 0, 0, 1), p(6, 6, 1, 0, 0, 0, 1))).isPositive();
	}

	// =========================================================================
	// Position — volume
	// =========================================================================

	/**
	 * {@code higherVolumeIsBetter()} prefers the larger box.
	 *
	 * <pre>
	 *  Isometric view:
	 *
	 *  A: 2×2×2 vol=8        B: 4×4×4 vol=64
	 *     ┌──┐                   ┌────┐
	 *    /  /│                  /    /│
	 *   └──┘ │                 └────┘ │  ← B preferred
	 *
	 *  Expected: compare(B, A) > 0
	 * </pre>
	 */
	@Test
	void higherVolumeIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherVolumeIsBetter().build();
		assertThat(cmp.compare(p(4, 4, 4, 0, 0, 0, 1), p(2, 2, 2, 0, 0, 0, 1))).isPositive();
	}

	@Test
	void lowerVolumeIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerVolumeIsBetter().build();
		assertThat(cmp.compare(p(2, 2, 2, 0, 0, 0, 1), p(4, 4, 4, 0, 0, 0, 1))).isPositive();
	}

	// =========================================================================
	// Position — box weight
	// =========================================================================

	/**
	 * {@code higherWeightIsBetter()} prefers the heavier box.
	 *
	 * <pre>
	 *  Side view (same size, different weight):
	 *
	 *  A: weight=50             B: weight=5
	 *  ┌────────────┐           ┌────────────┐
	 *  │ ████ heavy │           │   light    │
	 *  └────────────┘           └────────────┘
	 *        ↑ preferred
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void higherWeightIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherWeightIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 50), p(5, 5, 1, 0, 0, 0, 5))).isPositive();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 5), p(5, 5, 1, 0, 0, 0, 50))).isNegative();
	}

	@Test
	void lowerWeightIsBetter() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerWeightIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 5), p(5, 5, 1, 0, 0, 0, 50))).isPositive();
	}

	// =========================================================================
	// Position — support ratio
	// =========================================================================

	/**
	 * {@code higherSupportIsBetter()} prefers the more stable placement.
	 *
	 * <pre>
	 *  Overhead view — two 10×10 boxes:
	 *
	 *  A: supportedArea=30 (30%)       B: supportedArea=90 (90%)
	 *  ┌──────────┐                    ┌──────────┐
	 *  │░░░░██████│  30% supported     │██████████│  90% supported  ← preferred
	 *  │░░░░██████│                    │█████████ │
	 *  └──────────┘                    └──────────┘
	 *  ░=unsupported  █=supported
	 *
	 *  Expected: compare(B, A) > 0
	 * </pre>
	 */
	@Test
	void higherSupportIsBetter_higherRatioWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherSupportIsBetter().build();
		assertThat(cmp.compare(ps(10, 10, 1, 90), ps(10, 10, 1, 30))).isPositive();
		assertThat(cmp.compare(ps(10, 10, 1, 30), ps(10, 10, 1, 90))).isNegative();
	}

	/**
	 * Support ratio normalises across different box sizes.
	 *
	 * <pre>
	 *  A: 10×10 box, supportedArea=60  →  ratio=0.60
	 *  ┌──────────┐
	 *  │██████    │
	 *  │██████    │  60% covered
	 *  └──────────┘
	 *
	 *  B: 5×5 box, supportedArea=25  →  ratio=1.00  ← preferred
	 *  ┌─────┐
	 *  │█████│  100% covered
	 *  └─────┘
	 *
	 *  Expected: compare(B, A) > 0
	 * </pre>
	 */
	@Test
	void higherSupportIsBetter_normalisedAcrossDifferentBoxSizes() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherSupportIsBetter().build();
		assertThat(cmp.compare(ps(5, 5, 1, 25), ps(10, 10, 1, 60))).isPositive(); // 1.00 > 0.60
	}

	@Test
	void lowerSupportIsBetter_lowerRatioWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerSupportIsBetter().build();
		assertThat(cmp.compare(ps(10, 10, 1, 30), ps(10, 10, 1, 90))).isPositive();
	}

	// =========================================================================
	// Constraint — max-load weight
	// =========================================================================

	/**
	 * Placement whose box can carry more weight on top is preferred.
	 *
	 * <pre>
	 *  Side view — same size, different load-weight limits:
	 *
	 *  A: maxLoadWeight=500               B: maxLoadWeight=100
	 *  ┌──────────────────────┐           ┌──────────────────────┐
	 *  │  capacity: 500 kg ░░ │           │  capacity: 100 kg    │
	 *  └──────────────────────┘           └──────────────────────┘
	 *              ↑ preferred (more headroom)
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void higherMaxLoadWeightIsBetter_higherLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadWeightIsBetter().build();
		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 500), pMaxWeight(5, 5, 1, 100))).isPositive();
		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 100), pMaxWeight(5, 5, 1, 500))).isNegative();
	}

	/**
	 * Unconstrained (no weight limit) always beats any constrained box.
	 *
	 * <pre>
	 *  A: no weight limit (∞)             B: maxLoadWeight=300
	 *  ┌──────────────────────┐           ┌──────────────────────┐
	 *  │  capacity: ∞      ░░░│           │  capacity: 300 kg    │
	 *  └──────────────────────┘           └──────────────────────┘
	 *              ↑ preferred (unconstrained = better)
	 * </pre>
	 */
	@Test
	void higherMaxLoadWeightIsBetter_unconstrainedBeatsConstrained() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadWeightIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), pMaxWeight(5, 5, 1, 300))).isPositive();
	}

	@Test
	void lowerMaxLoadWeightIsBetter_lowerLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerMaxLoadWeightIsBetter().build();
		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 100), pMaxWeight(5, 5, 1, 500))).isPositive();
	}

	// =========================================================================
	// Constraint — max-load pressure
	// =========================================================================

	/**
	 * Placement whose box tolerates greater pressure on its top surface is preferred.
	 *
	 * <pre>
	 *  Side view — same footprint, different pressure tolerance:
	 *
	 *  A: maxLoadPressure=8.0 N/cm²       B: maxLoadPressure=2.0 N/cm²
	 *  ┌──────────────────────┐           ┌──────────────────────┐
	 *  │  pressure: 8.0  ▓▓▓▓ │           │  pressure: 2.0       │
	 *  └──────────────────────┘           └──────────────────────┘
	 *              ↑ preferred (tolerates more pressure)
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void higherMaxLoadPressureIsBetter_higherLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadPressureIsBetter().build();
		assertThat(cmp.compare(pMaxPressure(5, 5, 1, 8.0), pMaxPressure(5, 5, 1, 2.0))).isPositive();
	}

	@Test
	void higherMaxLoadPressureIsBetter_unconstrainedBeatsConstrained() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadPressureIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), pMaxPressure(5, 5, 1, 5.0))).isPositive();
	}

	@Test
	void lowerMaxLoadPressureIsBetter_lowerLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerMaxLoadPressureIsBetter().build();
		assertThat(cmp.compare(pMaxPressure(5, 5, 1, 2.0), pMaxPressure(5, 5, 1, 8.0))).isPositive();
	}

	// =========================================================================
	// Constraint — max-load box count
	// =========================================================================

	/**
	 * Placement whose box allows more stacked boxes on top is preferred.
	 *
	 * <pre>
	 *  Side view — same box, different stack-depth limits:
	 *
	 *  A: maxLoadBoxCount=5              B: maxLoadBoxCount=2
	 *  ┌────┐  ↑ up to 5                ┌────┐  ↑ up to 2
	 *  ┌────┐                           ┌────┐
	 *  ┌────┐                           └────┘  limit
	 *  ┌────┐
	 *  ┌────┐
	 *  └────┘  ← base (preferred)
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void higherMaxLoadBoxCountIsBetter_higherLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadBoxCountIsBetter().build();
		assertThat(cmp.compare(pMaxCount(5, 5, 1, 5), pMaxCount(5, 5, 1, 2))).isPositive();
		assertThat(cmp.compare(pMaxCount(5, 5, 1, 2), pMaxCount(5, 5, 1, 5))).isNegative();
	}

	@Test
	void higherMaxLoadBoxCountIsBetter_unconstrainedBeatsConstrained() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().higherMaxLoadBoxCountIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), pMaxCount(5, 5, 1, 3))).isPositive();
	}

	@Test
	void lowerMaxLoadBoxCountIsBetter_lowerLimitWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().lowerMaxLoadBoxCountIsBetter().build();
		assertThat(cmp.compare(pMaxCount(5, 5, 1, 2), pMaxCount(5, 5, 1, 5))).isPositive();
	}

	// =========================================================================
	// Constraint — identical-only restriction
	// =========================================================================

	/**
	 * Box without restriction (any type on top) beats box that only allows identical boxes.
	 *
	 * <pre>
	 *  Side view — same box, different stacking restriction:
	 *
	 *  A: accepts any box on top         B: identical boxes only
	 *  ┌──────────────────────┐           ┌──────────────────────┐
	 *  │  ■ ● ▲ all accepted  │           │  ■ ■ ■ same only     │
	 *  └──────────────────────┘           └──────────────────────┘
	 *              ↑ preferred (more future stacking options)
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void noIdenticalConstraintIsBetter_unrestrictedWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().noIdenticalConstraintIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), pIdentOnly(5, 5, 1))).isPositive();
		assertThat(cmp.compare(pIdentOnly(5, 5, 1), p(5, 5, 1, 0, 0, 0, 1))).isNegative();
	}

	@Test
	void noIdenticalConstraintIsBetter_bothUnrestricted_returnsZero() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().noIdenticalConstraintIsBetter().build();
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), p(3, 3, 2, 0, 0, 0, 1))).isZero();
	}

	@Test
	void identicalConstraintIsBetter_identicalWins() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory().identicalConstraintIsBetter().build();
		assertThat(cmp.compare(pIdentOnly(5, 5, 1), p(5, 5, 1, 0, 0, 0, 1))).isPositive();
	}

	// =========================================================================
	// Mixed chaining — position + constraint dimensions together
	// =========================================================================

	/**
	 * Constraint dimension is decisive even when followed by position dimensions.
	 *
	 * <pre>
	 *  Chain: noIdentical → lowerZ → higherArea
	 *
	 *  A: unrestricted, z=5, area=100    B: identical-only, z=0, area=900
	 *
	 *  noIdentical fires first: A wins despite B having lower z and larger area.
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void mixedChain_constraintBeforePosition_constraintDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.lowerZIsBetter()
				.higherAreaIsBetter()
				.build();

		Placement unrestricted = p(10, 10, 1, 0, 0, 5, 1); // no restriction, z=5, area=100
		Placement identOnly    = pIdentOnly(30, 30, 1);     // identical-only, area=900

		assertThat(cmp.compare(unrestricted, identOnly)).isPositive();
	}

	/**
	 * Position dimension decides when constraint dimension is a tie.
	 *
	 * <pre>
	 *  Chain: noIdentical → lowerZ → higherArea
	 *
	 *  Both unrestricted; z differs.
	 *
	 *  A: unrestricted, z=1    B: unrestricted, z=5
	 *  → z decides: A wins.
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void mixedChain_constraintTie_positionDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.lowerZIsBetter()
				.higherAreaIsBetter()
				.build();

		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 5, 1))).isPositive();
	}

	// =========================================================================
	// Chaining — all constraint dimensions
	// =========================================================================

	/**
	 * Full constraint chain: noIdentical → higherCount → higherWeight → higherPressure.
	 * Identical flag is decisive.
	 *
	 * <pre>
	 *  A: no restriction, count=2, weight=100, pressure=1.0
	 *  B: identical-only, count=5, weight=500, pressure=8.0
	 *
	 *  A wins despite B having larger headroom on all numeric constraints.
	 *
	 *  Expected: compare(A, B) > 0
	 * </pre>
	 */
	@Test
	void constraintChain_identicalDecides() {
		assertThat(constraintChain().compare(
				pAllConstraints(false, 2, 100, 1.0),
				pAllConstraints(true,  5, 500, 8.0))).isPositive();
	}

	/** Box-count decides when identical flags are equal. */
	@Test
	void constraintChain_boxCountDecides() {
		assertThat(constraintChain().compare(
				pAllConstraints(false, 5,  50, 1.0),
				pAllConstraints(false, 2, 500, 9.0))).isPositive();
	}

	/** Weight decides when identical flag and count are equal. */
	@Test
	void constraintChain_weightDecides() {
		assertThat(constraintChain().compare(
				pAllConstraints(false, 3, 300, 1.0),
				pAllConstraints(false, 3,  50, 9.0))).isPositive();
	}

	/** Pressure decides when all other constraint dimensions are equal. */
	@Test
	void constraintChain_pressureDecides() {
		assertThat(constraintChain().compare(
				pAllConstraints(false, 3, 100, 7.0),
				pAllConstraints(false, 3, 100, 1.5))).isPositive();
	}

	// =========================================================================
	// Chaining — all position dimensions
	// =========================================================================

	/** Full position chain: lowerZ → higherSupport → higherArea → higherVolume → lowerWeight. */
	@Test
	void positionChain_zDecides() {
		assertThat(positionChain().compare(
				ps(4, 4, 2, 16, 0, 0, 0, 5),
				ps(8, 8, 8, 64, 0, 0, 5, 5))).isPositive(); // z=0 wins over z=5
	}

	@Test
	void positionChain_supportDecides() {
		// same z=0; support ratio decides (both 10×10)
		assertThat(positionChain().compare(
				ps(10, 10, 1, 100, 0, 0, 0, 5),  // ratio=1.00
				ps(10, 10, 1,  40, 0, 0, 0, 5))).isPositive(); // ratio=0.40
	}

	@Test
	void positionChain_areaDecides() {
		// z=0, support=1.00; area decides
		assertThat(positionChain().compare(
				ps(6, 6, 1, 36, 0, 0, 0, 5), // area=36
				ps(3, 3, 4,  9, 0, 0, 0, 5))).isPositive(); // area=9
	}

	@Test
	void positionChain_volumeDecides() {
		// z=0, support=1.00, area=16; volume decides
		assertThat(positionChain().compare(
				ps(4, 4, 5, 16, 0, 0, 0, 5), // vol=80
				ps(4, 4, 2, 16, 0, 0, 0, 5))).isPositive(); // vol=32
	}

	@Test
	void positionChain_weightDecides() {
		// z=0, support=1.00, area=16, volume=64; weight decides (lower is better)
		assertThat(positionChain().compare(
				ps(4, 4, 4, 16, 0, 0, 0,  2),  // lighter
				ps(4, 4, 4, 16, 0, 0, 0, 10))).isPositive(); // heavier
	}

	// =========================================================================
	// forBoxItems static factory
	// =========================================================================

	/**
	 * {@code forBoxItems} with no constraints produces a no-op comparator.
	 *
	 * <pre>
	 *  Item list: [unconstrained box]
	 *
	 *  Expected: all placements equal → compare returns 0
	 * </pre>
	 */
	@Test
	void forBoxItems_noConstraints_noOp() {
		BoxItem item = new BoxItem(Box.newBuilder()
				.withId("plain").withSize(10, 10, 10).withWeight(1).build());

		PlacementComparator cmp = DefaultPlacementComparatorFactory.forBoxItems(List.of(item));

		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 100), pMaxWeight(5, 5, 1, 50))).isZero();
	}

	/**
	 * {@code forBoxItems} detects weight constraint and returns weight comparator.
	 *
	 * <pre>
	 *  Item list: [box with maxLoadWeight=100]
	 *
	 *  Expected: higher weight limit wins
	 * </pre>
	 */
	@Test
	void forBoxItems_weightOnly_higherWeightPreferred() {
		BoxItem item = new BoxItem(Box.newBuilder()
				.withId("heavy").withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(100).build());

		PlacementComparator cmp = DefaultPlacementComparatorFactory.forBoxItems(List.of(item));

		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 200), pMaxWeight(5, 5, 1, 50))).isPositive();
	}

	@Test
	void forBoxItems_allConstraints_identicalIsFirstPriority() {
		BoxItem item = new BoxItem(Box.newBuilder()
				.withId("all").withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(100).withMaxLoadPressure(5.0)
				.withMaxLoadIdenticalBoxCount(3).build());

		PlacementComparator cmp = DefaultPlacementComparatorFactory.forBoxItems(List.of(item));

		assertThat(cmp.compare(
				pAllConstraints(false, 2, 100, 1.0),
				pAllConstraints(true,  5, 500, 9.0))).isPositive();
	}

	// =========================================================================
	// Optimized registry — type and behaviour verification
	// =========================================================================

	/**
	 * When the exact stability-preset dimensions are added, the builder must return the fused
	 * comparator, not a four-link chain.
	 *
	 * <pre>
	 *  Dimensions added: higherSupport → higherVolume → higherWeight → lowerZ
	 *
	 *  Expected type: HigherSupportHigherVolumeHigherWeightLowerZComparator
	 *  (single object, no inner chain for these four dims)
	 * </pre>
	 */
	@Test
	void optimizedRegistry_stabilityPreset_returnsFusedType() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerZIsBetter()
				.build();

		assertThat(cmp).isInstanceOf(
				DefaultPlacementComparatorFactory.HigherSupportHigherVolumeHigherWeightLowerZComparator.class);
		// No inner chain needed for these four dimensions — next must be null.
		assertThat(((AbstractChainedPlacementComparator) cmp).getNext()).isNull();
	}

	/**
	 * Named preset convenience method produces the same type and behaviour as the manual chain.
	 */
	@Test
	void optimizedRegistry_namedPreset_sameAsFourMethodChain() {
		PlacementComparator manual = DefaultPlacementComparatorFactory.newFactory()
				.higherSupportIsBetter().higherVolumeIsBetter()
				.higherWeightIsBetter().lowerZIsBetter().build();
		PlacementComparator preset = DefaultPlacementComparatorFactory
				.higherSupportHigherVolumeHigherWeightLowerZ().build();

		Placement a = ps(10, 10, 4, 100, 0, 0, 0, 50);
		Placement b = ps(10, 10, 4,  60, 0, 0, 3, 10);

		assertThat(Integer.signum(preset.compare(a, b)))
				.isEqualTo(Integer.signum(manual.compare(a, b)));
	}

	/**
	 * Extra suffix dimension added after the preset prefix is chained as {@code next}.
	 *
	 * <pre>
	 *  Dimensions: higherSupport → higherVolume → higherWeight → lowerZ → lowerX
	 *
	 *  Expected: fused outer handles first four; LowerXComparator is chained as next.
	 * </pre>
	 */
	@Test
	void optimizedRegistry_prefixMatchWithSuffix_suffixIsChainedAsNext() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerZIsBetter()
				.lowerXIsBetter()
				.build();

		assertThat(cmp).isInstanceOf(
				DefaultPlacementComparatorFactory.HigherSupportHigherVolumeHigherWeightLowerZComparator.class);
		assertThat(((AbstractChainedPlacementComparator) cmp).getNext())
				.isInstanceOf(DefaultPlacementComparatorFactory.LowerXComparator.class);
	}

	/**
	 * Constraint-chain fused comparator is used by {@code forBoxItems} when all four constraint
	 * types are present.
	 *
	 * <pre>
	 *  Item has: maxLoadWeight + maxLoadPressure + maxLoadIdenticalBoxCount (implies identicalOnly)
	 *
	 *  Expected type: NoIdenticalHigherCountHigherWeightHigherPressureComparator
	 * </pre>
	 */
	@Test
	void optimizedRegistry_forBoxItems_allConstraints_returnsFusedType() {
		BoxItem item = new BoxItem(Box.newBuilder()
				.withId("all").withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(200).withMaxLoadPressure(5.0)
				.withMaxLoadIdenticalBoxCount(3).build());

		PlacementComparator cmp = DefaultPlacementComparatorFactory.forBoxItems(List.of(item));

		assertThat(cmp).isInstanceOf(
				DefaultPlacementComparatorFactory.NoIdenticalHigherCountHigherWeightHigherPressureComparator.class);
	}

	/**
	 * When dimensions do NOT match any registered prefix, the plain per-dimension chain is used.
	 *
	 * <pre>
	 *  Dimensions: lowerZ → higherArea   (no registered prefix matches this combo)
	 *
	 *  Expected: plain LowerZComparator chain (not a fused type)
	 * </pre>
	 */
	@Test
	void optimizedRegistry_noMatch_returnsPlainChain() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.lowerZIsBetter()
				.higherAreaIsBetter()
				.build();

		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
		assertThat(((AbstractChainedPlacementComparator) cmp).getNext())
				.isInstanceOf(DefaultPlacementComparatorFactory.HigherAreaComparator.class);
	}

	// =========================================================================
	// HigherSupportHigherVolumeHigherWeightLowerZComparator — per-priority tests
	// =========================================================================

	/**
	 * Support is decisive even when volume, weight and z all favour the other placement.
	 *
	 * <pre>
	 *  A: 10×10 box, support=100 (100%)   B: 10×10 box, support=30 (30%), vol=1000, w=99, z=0
	 *  ┌──────────┐  ← fully supported    ┌──────────┐
	 *  │██████████│                        │███░░░░░░░│  30% supported
	 *  └──────────┘                        └──────────┘
	 *  vol=200, w=1, z=5                   vol=1000, w=99, z=0
	 *
	 *  A wins despite smaller vol/weight/higher-z: support decides.
	 * </pre>
	 */
	@Test
	void fused_stability_supportDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory
				.higherSupportHigherVolumeHigherWeightLowerZ().build();

		Placement a = ps(10, 10, 2, 100, 0, 0, 5, 1);   // support=100%, vol=200, w=1,  z=5
		Placement b = ps(10, 10, 10, 30, 0, 0, 0, 99);  // support=30%,  vol=1000,w=99, z=0

		assertThat(cmp.compare(a, b)).isPositive(); // A wins on support
	}

	/**
	 * Volume decides when support is tied; weight and z both favour B.
	 *
	 * <pre>
	 *  Both 10×10 boxes, support=100%
	 *
	 *  A: vol=300 (10×10×3)       B: vol=100 (10×10×1), weight=99, z=0
	 *      ┌────────┐                 ┌────────┐
	 *      │ tall   │ 3 high          │ flat   │ 1 high
	 *      └────────┘                 └────────┘
	 *      preferred (larger vol)
	 * </pre>
	 */
	@Test
	void fused_stability_volumeDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory
				.higherSupportHigherVolumeHigherWeightLowerZ().build();

		Placement a = ps(10, 10, 3, 100, 0, 0, 3, 1);  // vol=300, w=1,  z=3
		Placement b = ps(10, 10, 1, 100, 0, 0, 0, 99); // vol=100, w=99, z=0

		assertThat(cmp.compare(a, b)).isPositive(); // A wins on volume
	}

	/**
	 * Weight decides when support and volume are tied; z favours B.
	 *
	 * <pre>
	 *  Both 10×10×2, support=100%:
	 *
	 *  A: weight=80 ██████████████    B: weight=5 ░░░░░░, z=0
	 *              ↑ preferred (heavier)
	 * </pre>
	 */
	@Test
	void fused_stability_weightDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory
				.higherSupportHigherVolumeHigherWeightLowerZ().build();

		Placement a = ps(10, 10, 2, 200, 0, 0, 5, 80); // vol=200, w=80, z=5
		Placement b = ps(10, 10, 2, 200, 0, 0, 0,  5); // vol=200, w=5,  z=0

		assertThat(cmp.compare(a, b)).isPositive(); // A wins on weight
	}

	/**
	 * Z decides when support, volume and weight are all tied.
	 *
	 * <pre>
	 *  Side view — identical boxes, different z:
	 *
	 *  z=1  ┌──────┐  ← A  (closer to floor, preferred)
	 *  z=5  ┌──────┐  ← B  (elevated)
	 *  z=0  ════════  floor
	 * </pre>
	 */
	@Test
	void fused_stability_zDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory
				.higherSupportHigherVolumeHigherWeightLowerZ().build();

		Placement a = ps(10, 10, 2, 200, 0, 0, 1, 10); // z=1
		Placement b = ps(10, 10, 2, 200, 0, 0, 5, 10); // z=5

		assertThat(cmp.compare(a, b)).isPositive(); // A wins on z
	}

	// =========================================================================
	// NoIdenticalHigherCountHigherWeightHigherPressureComparator — per-priority tests
	// =========================================================================

	/**
	 * Identical restriction is decisive even when count/weight/pressure all favour B.
	 *
	 * <pre>
	 *  A: no restriction  ■ ● ▲    B: identical-only  ■ ■ ■ (count=10, w=500, p=9)
	 *              ↑ preferred
	 * </pre>
	 */
	@Test
	void fused_constraintChain_identicalDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.higherMaxLoadPressureIsBetter()
				.build();

		assertThat(cmp.compare(
				pAllConstraints(false, 2, 100, 1.0),
				pAllConstraints(true,  10, 500, 9.0))).isPositive();
	}

	/**
	 * Box count decides when identical flags are tied.
	 *
	 * <pre>
	 *  Both unrestricted. A: count=8 → B: count=2 (weight/pressure favour B).
	 *
	 *  A: ┌──┬──┬──┬──┬──┬──┬──┬──┐  up to 8
	 *  B: ┌──┬──┐  up to 2
	 *  A preferred on count.
	 * </pre>
	 */
	@Test
	void fused_constraintChain_countDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.higherMaxLoadPressureIsBetter()
				.build();

		assertThat(cmp.compare(
				pAllConstraints(false, 8, 50, 1.0),
				pAllConstraints(false, 2, 500, 9.0))).isPositive();
	}

	/** Weight decides when identical and count are tied. */
	@Test
	void fused_constraintChain_weightDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.higherMaxLoadPressureIsBetter()
				.build();

		assertThat(cmp.compare(
				pAllConstraints(false, 3, 400, 1.0),
				pAllConstraints(false, 3,  50, 9.0))).isPositive();
	}

	/** Pressure decides when all other constraint dimensions are tied. */
	@Test
	void fused_constraintChain_pressureDecides() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.higherMaxLoadPressureIsBetter()
				.build();

		assertThat(cmp.compare(
				pAllConstraints(false, 3, 100, 8.0),
				pAllConstraints(false, 3, 100, 1.5))).isPositive();
	}

	// =========================================================================
	// Built comparator type checks
	// =========================================================================

	@Test
	void singleDimension_z_returnsCorrectSubclass() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory().lowerZIsBetter().build();
		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
	}

	@Test
	void singleDimension_weight_returnsCorrectSubclass() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.higherMaxLoadWeightIsBetter().build();
		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.HigherMaxLoadWeightComparator.class);
	}

	@Test
	void chain_outerIsFirstDimension_innerIsSecond() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.noIdenticalConstraintIsBetter()
				.lowerZIsBetter()
				.build();

		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.NoIdenticalConstraintComparator.class);
		assertThat(((AbstractChainedPlacementComparator) cmp).getNext())
				.isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
	}

	// =========================================================================
	// withConstraints() — clone with active constraint flags
	// =========================================================================

	/**
	 * {@code withConstraints(all false)} keeps only position entries (none here), producing noOp.
	 *
	 * <pre>
	 *  newBuilder()                   ← pre-loads 4 constraint entries
	 *  .withConstraints(false, false, false, false)
	 *  → no constraint entries in clone → noOp
	 *
	 *  A: maxWeight=500  B: maxWeight=100  → compare returns 0
	 * </pre>
	 */
	@Test
	void withConstraints_allFalse_producesNoOp() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.withConstraints(false, false, false, false).build();
		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 500), pMaxWeight(5, 5, 1, 100))).isZero();
	}

	/**
	 * {@code withConstraints(weight=true, rest=false)} retains only the weight entry.
	 *
	 * <pre>
	 *  newBuilder()                          ← pre-loads [identical, count, weight, pressure]
	 *  .withConstraints(true, false, false, false)
	 *  → clone contains only weight entry
	 *
	 *  A: maxWeight=500  B: maxWeight=100  → A preferred (weight active)
	 *  A: maxPressure=9.0  B: maxPressure=0.5  → tie (pressure not active)
	 * </pre>
	 */
	@Test
	void withConstraints_weightOnly_otherConstraintsExcluded() {
		DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newFactory();
		PlacementComparator cmp = template.withConstraints(true, false, false, false).build();

		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 500), pMaxWeight(5, 5, 1, 100))).isPositive();
		assertThat(cmp.compare(pMaxPressure(5, 5, 1, 9.0), pMaxPressure(5, 5, 1, 0.5))).isZero();
	}

	/**
	 * {@code withConstraints} preserves position entries regardless of flags.
	 *
	 * <pre>
	 *  Template: newBuilder() + lowerZIsBetter
	 *            entries: [identical, count, weight, pressure, lowerZ]
	 *
	 *  withConstraints(false, false, false, false)
	 *  → clone: [lowerZ]   (position entries always included)
	 *
	 *  A: z=1   B: z=8  → z decides (constraint entries absent but position fires)
	 * </pre>
	 */
	@Test
	void withConstraints_positionEntriesAlwaysIncluded() {
		DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newFactory().lowerZIsBetter();
		PlacementComparator cmp = template.withConstraints(false, false, false, false).build();

		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 8, 1))).isPositive();
	}

	/**
	 * {@code withConstraints} produces an independent clone; modifying the clone does not
	 * affect the template and vice-versa.
	 *
	 * <pre>
	 *  Template: newBuilder()  (4 constraint entries)
	 *  Clone A:  withConstraints(weight=true, rest=false)  → [weight]
	 *  Clone B:  withConstraints(count=true,  rest=false)  → [count]
	 *  Modifying clone A does not affect clone B.
	 * </pre>
	 */
	@Test
	void withConstraints_returnsIndependentClone() {
		DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newFactory();
		DefaultPlacementComparatorFactory cloneA = template.withConstraints(true, false, false, false);
		DefaultPlacementComparatorFactory cloneB = template.withConstraints(false, false, true, false);

		// Append a position dim to clone A — must not affect clone B
		cloneA.lowerZIsBetter();

		PlacementComparator cmpA = cloneA.build();
		PlacementComparator cmpB = cloneB.build();

		// cmpA has weight + lowerZ; count tie → z decides
		assertThat(cmpA.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 8, 1))).isPositive();
		// cmpB has only count; z does NOT influence it
		assertThat(cmpB.compare(pMaxCount(5, 5, 1, 5), pMaxCount(5, 5, 1, 1))).isPositive();
		assertThat(cmpB.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 8, 1))).isZero();
	}

	// =========================================================================
	// withoutConstraintType() — remove a constraint in-place
	// =========================================================================

	/**
	 * {@code withoutConstraintType(WEIGHT)} removes the weight entry from the builder.
	 *
	 * <pre>
	 *  newBuilder()               ← pre-loads [identical, count, weight, pressure]
	 *  .withoutAttribute(WEIGHT)
	 *  → builder contains [identical, count, pressure]
	 *
	 *  A: maxWeight=500  B: maxWeight=100  → tie (weight absent)
	 *  A: maxCount=10    B: maxCount=2     → A preferred (count still active)
	 * </pre>
	 */
	@Test
	void withoutConstraintType_removesWeightEntry() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT)
				.build();

		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 500), pMaxWeight(5, 5, 1, 100))).isZero();
		assertThat(cmp.compare(pMaxCount(5, 5, 1, 10), pMaxCount(5, 5, 1, 2))).isPositive();
	}

	/**
	 * Removing all constraint types makes the builder position-only.
	 *
	 * <pre>
	 *  newBuilder()
	 *  .withoutAttribute(IDENTICAL)
	 *  .withoutAttribute(COUNT)
	 *  .withoutAttribute(WEIGHT)
	 *  .withoutAttribute(PRESSURE)
	 *  .lowerZIsBetter()
	 *  → only lowerZ remains
	 *
	 *  A: z=1   B: z=8  → z decides
	 * </pre>
	 */
	@Test
	void withoutConstraintType_allRemoved_positionDimFires() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.withoutAttribute(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT)
				.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT)
				.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT)
				.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE)
				.lowerZIsBetter()
				.build();

		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 1, 1), p(5, 5, 1, 0, 0, 8, 1))).isPositive();
	}

	// =========================================================================
	// Constraint methods — replaceOrAdd semantics
	// =========================================================================

	/**
	 * Calling {@code higherMaxLoadWeightIsBetter()} on {@code newBuilder()} replaces the
	 * pre-loaded weight entry in-place, preserving priority order.
	 *
	 * <pre>
	 *  newBuilder()    → entries: [identical, count, weight(higher), pressure]
	 *  .higherMaxLoadWeightIsBetter()  → replaceOrAdd: same entry, order unchanged
	 *  .lowerMaxLoadWeightIsBetter()   → replaceOrAdd: REPLACES weight entry in-place
	 *
	 *  After lowerMaxLoadWeightIsBetter():
	 *  entries: [identical, count, weight(lower), pressure]
	 *
	 *  A: maxWeight=100 (smaller → lower is better)   B: maxWeight=500
	 *  Expected: A preferred (lower weight is now better)
	 * </pre>
	 */
	@Test
	void constraintMethod_replaceOrAdd_replacesExistingEntryInPlace() {
		// Default is higherMaxLoadWeightIsBetter; switch to lower
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.lowerMaxLoadWeightIsBetter()        // replaces the default WEIGHT entry in-place
				.withConstraints(true, false, false, false)
				.build();

		assertThat(cmp.compare(pMaxWeight(5, 5, 1, 100), pMaxWeight(5, 5, 1, 500))).isPositive();
	}

	/**
	 * Calling a constraint method on {@code emptyBuilder()} appends the entry (add semantics).
	 *
	 * <pre>
	 *  emptyBuilder() has no entries.
	 *  .higherMaxLoadBoxCountIsBetter() → appends COUNT entry
	 *  .higherMaxLoadWeightIsBetter()   → appends WEIGHT entry
	 *
	 *  Both count and weight are present; count fires first.
	 *
	 *  A: count=5, weight=50   B: count=2, weight=500  → A preferred (count decides)
	 * </pre>
	 */
	@Test
	void constraintMethod_replaceOrAdd_appendsWhenNotPresent() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.emptyFactory()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.build();

		assertThat(cmp.compare(
				pAllConstraints(false, 5, 50, 1.0),
				pAllConstraints(false, 2, 500, 9.0))).isPositive(); // count decides
	}

	/**
	 * Disable flags do not affect position dimension methods.
	 *
	 * <pre>
	 *  withConstraints(false, false, false, false) removes all constraint entries;
	 *  lowerZIsBetter is a position entry, always included.
	 *
	 *  A: z=0   B: z=5  → z decides (position dims unaffected by constraint filtering)
	 * </pre>
	 */
	@Test
	void withConstraints_doesNotAffectPositionDimensions() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.lowerZIsBetter()
				.withConstraints(false, false, false, false)
				.build();

		assertThat(cmp).isInstanceOf(DefaultPlacementComparatorFactory.LowerZComparator.class);
		assertThat(cmp.compare(p(5, 5, 1, 0, 0, 0, 1), p(5, 5, 1, 0, 0, 5, 1))).isPositive();
	}

	// =========================================================================
	// Symmetry
	// =========================================================================

	@Test
	void identicalPlacements_returnZero() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.lowerZIsBetter().higherAreaIsBetter().higherMaxLoadWeightIsBetter().build();
		Placement pp = p(5, 5, 1, 0, 0, 2, 10);
		assertThat(cmp.compare(pp, pp)).isZero();
	}

	@Test
	void antisymmetry() {
		PlacementComparator cmp = DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter().lowerZIsBetter().higherAreaIsBetter().build();
		Placement a = pAllConstraints(false, 5, 300, 7.0);
		Placement b = pAllConstraints(true,  2,  50, 1.5);
		assertThat(Integer.signum(cmp.compare(a, b))).isEqualTo(-Integer.signum(cmp.compare(b, a)));
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private static PlacementComparator constraintChain() {
		return DefaultPlacementComparatorFactory.newFactory()
				.noIdenticalConstraintIsBetter()
				.higherMaxLoadBoxCountIsBetter()
				.higherMaxLoadWeightIsBetter()
				.higherMaxLoadPressureIsBetter()
				.build();
	}

	private static PlacementComparator positionChain() {
		return DefaultPlacementComparatorFactory.emptyFactory()
				.lowerZIsBetter()
				.higherSupportIsBetter()
				.higherAreaIsBetter()
				.higherVolumeIsBetter()
				.lowerWeightIsBetter()
				.build();
	}

	/** Creates a placement for a {@code dx×dy×dz} box at {@code (x,y,z)} with given weight. */
	private static Placement p(int dx, int dy, int dz, int x, int y, int z, int weight) {
		Box box = Box.newBuilder()
				.withId("b-" + dx + "x" + dy + "x" + dz + "w" + weight)
				.withSize(dx, dy, dz).withWeight(weight).build();
		new BoxItem(box);
		Placement pl = new Placement(box.getStackValue(0), 0, x, y, z);
		pl.setSupportedArea(box.getStackValue(0).getArea());
		return pl;
	}

	/** Creates a placement with explicit supported area (at origin, weight=1). */
	private static Placement ps(int dx, int dy, int dz, long supportedArea) {
		return ps(dx, dy, dz, supportedArea, 0, 0, 0, 1);
	}

	/** Creates a placement with explicit supported area, position and weight. */
	private static Placement ps(int dx, int dy, int dz, long supportedArea,
			int x, int y, int z, int weight) {
		Box box = Box.newBuilder()
				.withId("b-" + dx + "x" + dy + "x" + dz + "s" + supportedArea + "w" + weight)
				.withSize(dx, dy, dz).withWeight(weight).build();
		new BoxItem(box);
		Placement pl = new Placement(box.getStackValue(0), 0, x, y, z);
		pl.setSupportedArea(supportedArea);
		return pl;
	}

	private static Placement pMaxWeight(int dx, int dy, int dz, long maxWeight) {
		Box box = Box.newBuilder().withId("b-mw" + maxWeight)
				.withSize(dx, dy, dz).withWeight(1).withMaxLoadWeight(maxWeight).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pMaxPressure(int dx, int dy, int dz, double maxPressure) {
		Box box = Box.newBuilder().withId("b-mp" + maxPressure)
				.withSize(dx, dy, dz).withWeight(1).withMaxLoadPressure(maxPressure).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pMaxCount(int dx, int dy, int dz, int maxCount) {
		Box box = Box.newBuilder().withId("b-mc" + maxCount)
				.withSize(dx, dy, dz).withWeight(1).withMaxLoadBoxCount(maxCount).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	/** Identical-only restriction, no count limit. */
	private static Placement pIdentOnly(int dx, int dy, int dz) {
		Box box = Box.newBuilder().withId("b-identical")
				.withSize(dx, dy, dz).withWeight(1)
				.withMaxLoadIdenticalBoxCount(-1).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	/**
	 * All four load constraints set.
	 *
	 * @param identicalOnly {@code true} to add identical-only restriction
	 * @param maxCount      max box count on top
	 * @param maxWeight     max load weight on top
	 * @param maxPressure   max load pressure on top
	 */
	private static Placement pAllConstraints(boolean identicalOnly, int maxCount,
			long maxWeight, double maxPressure) {
		Box.Builder builder = Box.newBuilder()
				.withId("b-i" + identicalOnly + "c" + maxCount + "w" + maxWeight + "p" + maxPressure)
				.withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(maxWeight)
				.withMaxLoadPressure(maxPressure);
		if (identicalOnly) {
			builder.withMaxLoadIdenticalBoxCount(maxCount);
		} else {
			builder.withMaxLoadBoxCount(maxCount);
		}
		Box box = builder.build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}
}
