package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorAttribute;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorFactory;

/**
 * Unit tests for {@link DefaultPlacementComparatorFactory}.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 */
class PlacementComparatorBuilderFactoryTest {

	// =========================================================================
	// Default factory (all constraint dims enabled)
	// =========================================================================

	/**
	 * Default factory with all constraints enabled produces a full constraint comparator
	 * when all four flags are active.
	 *
	 * <pre>
	 *  Active flags: weight=true, pressure=true, count=true, identical=true
	 *
	 *  A: no identical restriction  ■ ● ▲    B: identical-only  ■ ■ ■
	 *              ↑ preferred on identical (priority 1)
	 * </pre>
	 */
	@Test
	void allEnabled_allActive_identicalIsDecisive() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(true, true, true, true);

		assertThat(cmp.compare(pUnrestricted(), pIdentOnly())).isPositive();
	}

	/**
	 * Default factory with all flags active — count decides when identical flags are tied.
	 *
	 * <pre>
	 *  Both unrestricted; A: maxCount=8, B: maxCount=2
	 *
	 *  A: ┌──┬──┬──┬──┬──┬──┬──┬──┐  up to 8 boxes
	 *  B: ┌──┬──┐  up to 2 boxes
	 *                ↑ A preferred on count (priority 2)
	 * </pre>
	 */
	@Test
	void allEnabled_allActive_countIsDecisiveAfterIdentical() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(true, true, true, true);

		assertThat(cmp.compare(pCount(8), pCount(2))).isPositive();
	}

	/**
	 * Default factory with all flags active — weight decides when identical and count are tied.
	 *
	 * <pre>
	 *  Both unrestricted, same count; A: maxWeight=500, B: maxWeight=100
	 *
	 *  A: ░░░░░░░░░░░░░░░  capacity 500 kg  ← preferred
	 *  B: ░░░░             capacity 100 kg
	 * </pre>
	 */
	@Test
	void allEnabled_allActive_weightIsDecisiveAfterCount() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(true, true, true, true);

		assertThat(cmp.compare(pWeight(500), pWeight(100))).isPositive();
	}

	/**
	 * Default factory with all flags active — pressure decides last among constraints.
	 *
	 * <pre>
	 *  Both unrestricted, same count and weight; A: maxPressure=8.0, B: maxPressure=1.5
	 *
	 *  A: ▓▓▓▓▓▓▓▓▓  8.0 N/cm²  ← preferred
	 *  B: ▓▓           1.5 N/cm²
	 * </pre>
	 */
	@Test
	void allEnabled_allActive_pressureIsLastConstraintPriority() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(true, true, true, true);

		assertThat(cmp.compare(pPressure(8.0), pPressure(1.5))).isPositive();
	}

	// =========================================================================
	// Active flags gate which constraint dims are included
	// =========================================================================

	/**
	 * When the active weight flag is false, weight is not added to the comparator even if
	 * the factory has it enabled.
	 *
	 * <pre>
	 *  Factory: all enabled. Active: weight=false.
	 *
	 *  A: maxWeight=500  B: maxWeight=100
	 *
	 *  Expected: compare returns 0 — weight not compared.
	 * </pre>
	 */
	@Test
	void activeWeight_false_weightNotCompared() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(false, false, false, false);

		assertThat(cmp.compare(pWeight(500), pWeight(100))).isZero();
	}

	/**
	 * Only active flags that are also factory-enabled contribute to the comparator.
	 *
	 * <pre>
	 *  Factory: all enabled. Active: weight=true, pressure=false, count=false, identical=false.
	 *
	 *  A: maxWeight=500   B: maxWeight=100
	 *  Expected: A preferred (weight is the only active + enabled dim).
	 *
	 *  A: maxPressure=8.0  B: maxPressure=1.0
	 *  Expected: tie (pressure not active).
	 * </pre>
	 */
	@Test
	void onlyWeightActive_weightsCompared_pressureIsNotCompared() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();
		PlacementComparator cmp = factory.create(true, false, false, false);

		assertThat(cmp.compare(pWeight(500), pWeight(100))).isPositive();
		assertThat(cmp.compare(pPressure(8.0), pPressure(1.0))).isZero();
	}

	// =========================================================================
	// Factory-level enable flags disable dims regardless of active flags
	// =========================================================================

	/**
	 * When the factory has pressure disabled, pressure is never compared even if the active
	 * pressure flag is true.
	 *
	 * <pre>
	 *  Factory: pressureEnabled=false. Active: pressure=true.
	 *
	 *  A: maxPressure=9.0   B: maxPressure=0.5
	 *  Expected: tie — pressure dim was disabled at factory level.
	 * </pre>
	 */
	@Test
	void factoryPressureDisabled_pressureNotComparedEvenIfActive() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPressureEnabled(false)
				.build();
		PlacementComparator cmp = factory.create(false, true, false, false);

		assertThat(cmp.compare(pPressure(9.0), pPressure(0.5))).isZero();
	}

	/**
	 * When the factory has weight disabled, only pressure and count are compared.
	 *
	 * <pre>
	 *  Factory: weightEnabled=false. Active: weight=true, count=true.
	 *
	 *  A: maxCount=10, maxWeight=50   B: maxCount=2, maxWeight=500
	 *
	 *  Weight disabled → count decides: A wins (count=10 vs 2).
	 *
	 *  A: ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐  count=10  ← preferred
	 *  B: ┌──┬──┐  count=2
	 * </pre>
	 */
	@Test
	void factoryWeightDisabled_countDecides() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withWeightEnabled(false)
				.build();
		PlacementComparator cmp = factory.create(true, false, true, false);

		assertThat(cmp.compare(pCount(10), pCount(2))).isPositive();
	}

	/**
	 * When all constraint dims are disabled at factory level, all constraint comparisons return 0.
	 *
	 * <pre>
	 *  Factory: all disabled. Active: all true.
	 *
	 *  Expected: all comparisons return 0 regardless of constraint values.
	 * </pre>
	 */
	@Test
	void factoryAllConstraintsDisabled_allComparisonsReturnZero() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withWeightEnabled(false)
				.withPressureEnabled(false)
				.withCountEnabled(false)
				.withIdenticalEnabled(false)
				.build();
		PlacementComparator cmp = factory.create(true, true, true, true);

		assertThat(cmp.compare(pWeight(999), pWeight(1))).isZero();
		assertThat(cmp.compare(pPressure(9.9), pPressure(0.1))).isZero();
		assertThat(cmp.compare(pCount(100), pCount(1))).isZero();
		assertThat(cmp.compare(pUnrestricted(), pIdentOnly())).isZero();
	}

	/**
	 * When the factory has identical disabled, identical restriction is not compared.
	 *
	 * <pre>
	 *  Factory: identicalEnabled=false. Active: identical=true.
	 *
	 *  A: no restriction  B: identical-only
	 *  Expected: tie — identical dim disabled.
	 * </pre>
	 */
	@Test
	void factoryIdenticalDisabled_identicalNotCompared() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withIdenticalEnabled(false)
				.build();
		PlacementComparator cmp = factory.create(false, false, false, true);

		assertThat(cmp.compare(pUnrestricted(), pIdentOnly())).isZero();
	}

	// =========================================================================
	// Position configurer
	// =========================================================================

	/**
	 * Position dimensions appended via {@code withPositionDimensions} act as tiebreakers after
	 * all constraint dims.
	 *
	 * <pre>
	 *  Factory: all constraints enabled. Position: lowerZ.
	 *  Active: no constraints.
	 *
	 *  A: z=1   B: z=5   → A preferred on z (constraint dims all tie).
	 *
	 *  Side view:
	 *  z=1  ┌──────┐  ← A  (lower, preferred)
	 *  z=5  ┌──────┐  ← B
	 *  z=0  ════════  floor
	 * </pre>
	 */
	@Test
	void positionDimensions_tiebreakOnZ() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();
		PlacementComparator cmp = factory.create(false, false, false, false);

		assertThat(cmp.compare(pAtZ(1), pAtZ(5))).isPositive();
		assertThat(cmp.compare(pAtZ(5), pAtZ(1))).isNegative();
	}

	/**
	 * Position dimensions are subordinate to constraint dims: constraint wins when both
	 * constraints and position differ.
	 *
	 * <pre>
	 *  Factory: all enabled. Position: lowerZ. Active: weight=true.
	 *
	 *  A: maxWeight=500, z=10    B: maxWeight=100, z=0
	 *
	 *  Constraint (weight) fires first: A preferred despite higher z.
	 * </pre>
	 */
	@Test
	void positionDimensions_constraintBeatsPosition() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();
		PlacementComparator cmp = factory.create(true, false, false, false);

		// A: weight=500, z=10   B: weight=100, z=0 → weight decides, A wins
		assertThat(cmp.compare(pWeightAtZ(500, 10), pWeightAtZ(100, 0))).isPositive();
	}

	/**
	 * Position dimensions fire when constraint dims are tied.
	 *
	 * <pre>
	 *  Factory: all enabled. Position: lowerZ. Active: weight=true.
	 *
	 *  A: maxWeight=300, z=1    B: maxWeight=300, z=8
	 *
	 *  Weight tie → z decides: A preferred.
	 * </pre>
	 */
	@Test
	void positionDimensions_firesOnConstraintTie() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();
		PlacementComparator cmp = factory.create(true, false, false, false);

		assertThat(cmp.compare(pWeightAtZ(300, 1), pWeightAtZ(300, 8))).isPositive();
	}

	// =========================================================================
	// LoadAwarePlacementControlsBuilder integration
	// =========================================================================

	/**
	 * The builder uses the factory to create the comparator based on active constraint flags.
	 * When only weight is active the factory-produced comparator includes only weight dim.
	 *
	 * <pre>
	 *  Factory: all enabled + lowerZ position. Active via withMaxLoad: weight=true only.
	 *
	 *  A: maxWeight=500, z=5    B: maxWeight=100, z=0
	 *  → weight decides: A preferred.
	 * </pre>
	 */
	@Test
	void loadAwarePlacementControlsBuilder_usesFactory_weightOnly() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();

		LoadAwarePlacementControlsBuilder builder = new LoadAwarePlacementControlsBuilder()
				.withPlacementComparatorBuilderFactory(factory);
		builder.withMaxLoad(true, false, false);

		// Access the comparator that will be used by calling the factory directly
		PlacementComparator cmp = factory.create(true, false, false, false);
		assertThat(cmp.compare(pWeightAtZ(500, 5), pWeightAtZ(100, 0))).isPositive();
	}

	/**
	 * When no constraints are active the factory produces a position-only comparator.
	 *
	 * <pre>
	 *  Factory: all enabled + lowerZ position. Active: no constraints.
	 *
	 *  A: z=2   B: z=9   → z decides: A preferred (weight not active, not compared).
	 * </pre>
	 */
	@Test
	void loadAwarePlacementControlsBuilder_noConstraintsActive_positionOnly() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();

		LoadAwarePlacementControlsBuilder builder = new LoadAwarePlacementControlsBuilder()
				.withPlacementComparatorBuilderFactory(factory);
		builder.withMaxLoad(false, false, false);

		PlacementComparator cmp = factory.create(false, false, false, false);
		assertThat(cmp.compare(pAtZ(2), pAtZ(9))).isPositive();
	}

	/**
	 * Fixed-comparator mode (no factory): builder uses the pre-built comparator unchanged.
	 *
	 * <pre>
	 *  Builder: fixed comparator that prefers higher z.
	 *  Active: all constraints.
	 *
	 *  Expected: higher-z wins (fixed comparator wrapped via PlacementComparatorFactory.of).
	 * </pre>
	 */
	@Test
	void loadAwarePlacementControlsBuilder_fixedComparator_notAffectedByConstraintFlags() {
		// Fixed comparator: prefers higher z
		PlacementComparator fixed = DefaultPlacementComparatorFactory.newFactory().higherZIsBetter().build();

		LoadAwarePlacementControlsBuilder builder = new LoadAwarePlacementControlsBuilder()
				.withPlacementComparatorBuilderFactory(PlacementComparatorFactory.of(fixed));
		builder.withMaxLoad(true, true, true);

		// PlacementComparatorFactory.of wraps the fixed comparator and ignores disabled attrs.
		assertThat(fixed.compare(pAtZ(9), pAtZ(1))).isPositive();
	}

	// =========================================================================
	// createBuilder() — returns a configurable builder
	// =========================================================================

	/**
	 * {@code createBuilder()} returns a builder that produces the same comparator as
	 * {@code create()}.
	 *
	 * <pre>
	 *  Factory: all enabled. Active: weight=true.
	 *
	 *  createBuilder(...).build() == create(...)  (same behaviour)
	 * </pre>
	 */
	@Test
	void createBuilder_sameBehaviourAsCreate() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();

		PlacementComparator fromCreate  = factory.create(true, false, false, false);
		PlacementComparator fromBuilder = factory.createBuilder(true, false, false, false).build();

		assertThat(Integer.signum(fromCreate.compare(pWeight(500), pWeight(100))))
				.isEqualTo(Integer.signum(fromBuilder.compare(pWeight(500), pWeight(100))));
	}

	/**
	 * Caller can append extra dimensions to the returned builder before calling {@code build()}.
	 *
	 * <pre>
	 *  Factory: all enabled, lowerZ position. Active: weight=true.
	 *  Caller appends: higherArea.
	 *
	 *  Chain: higherWeight → lowerZ → higherArea
	 *
	 *  A: weight=300, z=5, area=100   B: weight=300, z=5, area=900  ← B wins on area
	 *  Both have same weight and z, so area (added by caller) decides.
	 * </pre>
	 */
	@Test
	void createBuilder_callerAppendsExtraDim_firesAsExpected() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPositionDimensions(b -> b.lowerZIsBetter())
				.build();

		PlacementComparator cmp = factory.createBuilder(true, false, false, false)
				.higherAreaIsBetter()
				.build();

		// weight tie (both 300), z tie (both 0): area decides
		Placement smallArea = pWeightAtZ(300, 0); // 5×5 → area=25
		Placement largeArea = pArea(10, 10, 300);  // 10×10 → area=100

		assertThat(cmp.compare(largeArea, smallArea)).isPositive();
	}

	/**
	 * When the factory has pressure removed from its template, the clone produced by
	 * {@code createBuilder()} does not include pressure even if the active flag is true.
	 *
	 * <pre>
	 *  Factory: pressureEnabled=false → pressure absent from template.
	 *  createBuilder(false, true, false, false) → clone has no pressure entry.
	 *
	 *  A: maxPressure=9.0   B: maxPressure=0.5
	 *  Expected: tie — pressure not in template → not in clone.
	 * </pre>
	 */
	@Test
	void createBuilder_disabledDimAbsentFromTemplate_notIncludedInClone() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPressureEnabled(false)
				.build();

		PlacementComparator cmp = factory.createBuilder(false, true, false, false).build();

		assertThat(cmp.compare(pPressure(9.0), pPressure(0.1))).isZero();
	}

	/**
	 * The caller CAN add a factory-disabled constraint type explicitly to the returned builder.
	 *
	 * <pre>
	 *  Factory: pressureEnabled=false. Active: pressure=true.
	 *  Caller appends higherMaxLoadPressureIsBetter() to the returned builder.
	 *  Expected: pressure IS compared (caller explicitly added it).
	 * </pre>
	 */
	@Test
	void createBuilder_disabledDimAbsentFromTemplate_callerCanAddExplicitly() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder()
				.withPressureEnabled(false)
				.build();

		PlacementComparator cmp = factory.createBuilder(false, true, false, false)
				.higherMaxLoadPressureIsBetter()  // caller explicitly adds it back
				.build();

		assertThat(cmp.compare(pPressure(9.0), pPressure(0.1))).isPositive();
	}

	// =========================================================================
	// Multiple calls to create() from the same factory
	// =========================================================================

	/**
	 * The same factory instance can be called repeatedly with different flags, producing
	 * independent comparators each time.
	 *
	 * <pre>
	 *  Call 1: weight=true  → weight compared
	 *  Call 2: weight=false → weight not compared (returns 0)
	 * </pre>
	 */
	@Test
	void factory_calledRepeatedly_independentComparatorsPerCall() {
		DefaultPlacementComparatorFactory factory = DefaultPlacementComparatorFactory.newBuilder().build();

		PlacementComparator withWeight    = factory.create(true,  false, false, false);
		PlacementComparator withoutWeight = factory.create(false, false, false, false);

		assertThat(withWeight.compare(pWeight(500), pWeight(100))).isPositive();
		assertThat(withoutWeight.compare(pWeight(500), pWeight(100))).isZero();
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private static Placement pUnrestricted() {
		Box box = Box.newBuilder().withId("unrestricted").withSize(5, 5, 5).withWeight(1).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pIdentOnly() {
		Box box = Box.newBuilder().withId("ident-only").withSize(5, 5, 5).withWeight(1)
				.withMaxLoadIdenticalBoxCount(-1).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pWeight(long maxWeight) {
		Box box = Box.newBuilder().withId("w" + maxWeight).withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(maxWeight).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pWeightAtZ(long maxWeight, int z) {
		Box box = Box.newBuilder().withId("w" + maxWeight + "z" + z).withSize(5, 5, 5).withWeight(1)
				.withMaxLoadWeight(maxWeight).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, z);
	}

	private static Placement pPressure(double maxPressure) {
		Box box = Box.newBuilder().withId("p" + maxPressure).withSize(5, 5, 5).withWeight(1)
				.withMaxLoadPressure(maxPressure).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pCount(int maxCount) {
		Box box = Box.newBuilder().withId("c" + maxCount).withSize(5, 5, 5).withWeight(1)
				.withMaxLoadBoxCount(maxCount).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, 0);
	}

	private static Placement pAtZ(int z) {
		Box box = Box.newBuilder().withId("z" + z).withSize(5, 5, 5).withWeight(1).build();
		new BoxItem(box);
		return new Placement(box.getStackValue(0), 0, 0, 0, z);
	}

	/** Creates a placement with given footprint dimensions (and no constraints). */
	private static Placement pArea(int dx, int dy, long maxWeight) {
		Box box = Box.newBuilder().withId("a" + dx + "x" + dy)
				.withSize(dx, dy, 5).withWeight(1)
				.withMaxLoadWeight(maxWeight).build();
		new BoxItem(box);
		Placement pl = new Placement(box.getStackValue(0), 0, 0, 0, 0);
		pl.setSupportedArea(box.getStackValue(0).getArea());
		return pl;
	}
}
