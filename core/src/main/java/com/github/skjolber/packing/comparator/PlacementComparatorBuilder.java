package com.github.skjolber.packing.comparator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;

/**
 * Fluent builder for {@link PlacementComparator} instances.
 *
 * <p>Each builder method appends one dimension to the comparison chain. Dimensions are evaluated
 * in the order they are added; the first non-zero result wins.
 *
 * <p>At {@link #build()} time the builder scans a static <em>optimized registry</em> for the
 * longest known dimension prefix. When a match is found, the corresponding fused
 * {@link AbstractChainedPlacementComparator} subclass is instantiated (handling all matched
 * dimensions in a single {@code compare()} body), with the remaining suffix dimensions chained
 * as its {@code next}. When no prefix matches, a plain per-dimension chain is produced.
 * Either way no {@code List} is traversed at runtime inside {@code compare()}.
 *
 * <h3>Position and size dimensions</h3>
 * <ul>
 *   <li><b>x, y, z</b> — absolute coordinates of the placement origin.</li>
 *   <li><b>area</b> — footprint area of the box ({@code dx × dy}).</li>
 *   <li><b>volume</b> — volume of the box ({@code dx × dy × dz}).</li>
 *   <li><b>weight</b> — weight of the box itself.</li>
 *   <li><b>support</b> — ratio of physically supported area to total footprint area
 *       ({@code supportedArea / (dx × dy)}). A ratio of 1.0 means fully supported; lower values
 *       indicate partial overhang. Boxes of different sizes are compared by ratio.</li>
 * </ul>
 *
 * <h3>Load-constraint dimensions</h3>
 * <ul>
 *   <li><b>maxLoadWeight</b> — max weight the box can carry on top; unconstrained boxes are
 *       treated as {@link Long#MAX_VALUE} (unlimited) and always outrank constrained ones.</li>
 *   <li><b>maxLoadPressure</b> — max load pressure the box can carry; unconstrained boxes are
 *       treated as {@link Double#MAX_VALUE}.</li>
 *   <li><b>maxLoadBoxCount</b> — max number of boxes that may be stacked on top; unconstrained
 *       boxes are treated as {@link Integer#MAX_VALUE}.</li>
 *   <li><b>identicalOnly</b> — whether the box restricts stacking to identical boxes only;
 *       unrestricted boxes are preferred when {@link #noIdenticalConstraintIsBetter()} is used.</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>
 * // Prefer: no identical restriction → highest weight limit → lowest z → largest area
 * PlacementComparator cmp = PlacementComparatorBuilder.newBuilder()
 *         .noIdenticalConstraintIsBetter()
 *         .higherMaxLoadWeightIsBetter()
 *         .lowerZIsBetter()
 *         .higherAreaIsBetter()
 *         .build();
 * </pre>
 *
 * <h3>Named presets</h3>
 * Static factory methods pre-configure the builder with well-known dimension sets. These always
 * resolve to an optimized fused comparator at {@link #build()} time:
 * <ul>
 *   <li>{@link #higherSupportHigherVolumeHigherWeightLowerZ()} — placement stability strategy</li>
 * </ul>
 *
 * <p>The static {@link #forBoxItems(List)} factory method auto-detects active load constraints
 * from a list of box items and builds a natural-priority comparator
 * ({@link #noIdenticalConstraintIsBetter()} → {@link #higherMaxLoadBoxCountIsBetter()} →
 * {@link #higherMaxLoadWeightIsBetter()} → {@link #higherMaxLoadPressureIsBetter()}).
 *
 * <p>To combine two independently built comparators use
 * {@link PlacementComparator#thenComparing} or {@link CompositePlacementComparator}.
 */
public final class PlacementComparatorBuilder {

	// =========================================================================
	// Dim enum — one value per dimension + direction
	// =========================================================================

	/**
	 * Identifies one comparison dimension and its preferred direction.
	 * Used as a key in the optimized-registry prefix lookup.
	 */
	public enum Dim {
		LOWER_X, HIGHER_X,
		LOWER_Y, HIGHER_Y,
		LOWER_Z, HIGHER_Z,
		HIGHER_AREA, LOWER_AREA,
		HIGHER_VOLUME, LOWER_VOLUME,
		HIGHER_WEIGHT, LOWER_WEIGHT,
		HIGHER_SUPPORT, LOWER_SUPPORT,
		HIGHER_MAX_LOAD_WEIGHT, LOWER_MAX_LOAD_WEIGHT,
		HIGHER_MAX_LOAD_PRESSURE, LOWER_MAX_LOAD_PRESSURE,
		HIGHER_MAX_LOAD_BOX_COUNT, LOWER_MAX_LOAD_BOX_COUNT,
		NO_IDENTICAL_CONSTRAINT, IDENTICAL_CONSTRAINT
	}

	// =========================================================================
	// Optimized registry
	//
	// Each entry maps an ordered list of Dim values (a dimension prefix) to a
	// factory for the corresponding fused comparator. Entries are sorted longest-
	// first so that the most specific match wins.
	// =========================================================================

	private static final List<Map.Entry<List<Dim>, Function<PlacementComparator, AbstractChainedPlacementComparator>>>
			OPTIMIZED_REGISTRY;

	static {
		List<Map.Entry<List<Dim>, Function<PlacementComparator, AbstractChainedPlacementComparator>>> reg
				= new ArrayList<>();

		// high support → high volume → high weight → low z  (placement stability strategy)
		reg.add(new AbstractMap.SimpleEntry<>(
				List.of(Dim.HIGHER_SUPPORT, Dim.HIGHER_VOLUME, Dim.HIGHER_WEIGHT, Dim.LOWER_Z),
				HigherSupportHigherVolumeHigherWeightLowerZComparator::new));

		// no-identical → high box count → high weight → high pressure  (constraint chain / forBoxItems)
		reg.add(new AbstractMap.SimpleEntry<>(
				List.of(Dim.NO_IDENTICAL_CONSTRAINT, Dim.HIGHER_MAX_LOAD_BOX_COUNT,
						Dim.HIGHER_MAX_LOAD_WEIGHT, Dim.HIGHER_MAX_LOAD_PRESSURE),
				NoIdenticalHigherCountHigherWeightHigherPressureComparator::new));

		// Sort longest prefix first so the most specific match wins.
		reg.sort((e1, e2) -> Integer.compare(e2.getKey().size(), e1.getKey().size()));
		OPTIMIZED_REGISTRY = Collections.unmodifiableList(reg);
	}

	// =========================================================================
	// Builder state
	// =========================================================================

	/** Ordered list of dimension keys — parallel to {@link #factories}. */
	private final List<Dim> dims = new ArrayList<>();

	/** Ordered list of factories — parallel to {@link #dims}. */
	private final List<Function<PlacementComparator, AbstractChainedPlacementComparator>> factories
			= new ArrayList<>();

	private PlacementComparatorBuilder() {}

	/** Returns a new, empty builder. */
	public static PlacementComparatorBuilder newBuilder() {
		return new PlacementComparatorBuilder();
	}

	// =========================================================================
	// Named presets
	// =========================================================================

	/**
	 * Returns a builder pre-configured with the <em>placement stability</em> strategy:
	 * higher support ratio → higher volume → heavier box → lower z.
	 *
	 * <pre>
	 *  Priority (highest to lowest):
	 *   1. HIGHER_SUPPORT  — prefer placements with more area supported from below
	 *   2. HIGHER_VOLUME   — among equally stable, prefer larger boxes first
	 *   3. HIGHER_WEIGHT   — heavier boxes are placed before lighter ones
	 *   4. LOWER_Z         — when all else equal, prefer placements closer to the floor
	 * </pre>
	 *
	 * <p>This preset always resolves to {@link HigherSupportHigherVolumeHigherWeightLowerZComparator}
	 * at {@link #build()} time.
	 *
	 * @return a builder with the stability dimensions pre-added
	 */
	public static PlacementComparatorBuilder higherSupportHigherVolumeHigherWeightLowerZ() {
		return newBuilder()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerZIsBetter();
	}

	// =========================================================================
	// X coordinate
	// =========================================================================

	/** Prefer placements with a <b>lower x</b> coordinate. */
	public PlacementComparatorBuilder lowerXIsBetter() {
		dims.add(Dim.LOWER_X);
		factories.add(LowerXComparator::new);
		return this;
	}

	/** Prefer placements with a <b>higher x</b> coordinate. */
	public PlacementComparatorBuilder higherXIsBetter() {
		dims.add(Dim.HIGHER_X);
		factories.add(HigherXComparator::new);
		return this;
	}

	// =========================================================================
	// Y coordinate
	// =========================================================================

	/** Prefer placements with a <b>lower y</b> coordinate. */
	public PlacementComparatorBuilder lowerYIsBetter() {
		dims.add(Dim.LOWER_Y);
		factories.add(LowerYComparator::new);
		return this;
	}

	/** Prefer placements with a <b>higher y</b> coordinate. */
	public PlacementComparatorBuilder higherYIsBetter() {
		dims.add(Dim.HIGHER_Y);
		factories.add(HigherYComparator::new);
		return this;
	}

	// =========================================================================
	// Z coordinate
	// =========================================================================

	/**
	 * Prefer placements with a <b>lower z</b> coordinate (floor-first strategy).
	 * Use this to place boxes as close to the container floor as possible.
	 */
	public PlacementComparatorBuilder lowerZIsBetter() {
		dims.add(Dim.LOWER_Z);
		factories.add(LowerZComparator::new);
		return this;
	}

	/** Prefer placements with a <b>higher z</b> coordinate. */
	public PlacementComparatorBuilder higherZIsBetter() {
		dims.add(Dim.HIGHER_Z);
		factories.add(HigherZComparator::new);
		return this;
	}

	// =========================================================================
	// Footprint area  (dx × dy)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger footprint area</b> ({@code dx × dy}). */
	public PlacementComparatorBuilder higherAreaIsBetter() {
		dims.add(Dim.HIGHER_AREA);
		factories.add(HigherAreaComparator::new);
		return this;
	}

	/** Prefer placements where the box has a <b>smaller footprint area</b> ({@code dx × dy}). */
	public PlacementComparatorBuilder lowerAreaIsBetter() {
		dims.add(Dim.LOWER_AREA);
		factories.add(LowerAreaComparator::new);
		return this;
	}

	// =========================================================================
	// Volume  (dx × dy × dz)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger volume</b>. */
	public PlacementComparatorBuilder higherVolumeIsBetter() {
		dims.add(Dim.HIGHER_VOLUME);
		factories.add(HigherVolumeComparator::new);
		return this;
	}

	/** Prefer placements where the box has a <b>smaller volume</b>. */
	public PlacementComparatorBuilder lowerVolumeIsBetter() {
		dims.add(Dim.LOWER_VOLUME);
		factories.add(LowerVolumeComparator::new);
		return this;
	}

	// =========================================================================
	// Box weight
	// =========================================================================

	/** Prefer placements where the box is <b>heavier</b>. */
	public PlacementComparatorBuilder higherWeightIsBetter() {
		dims.add(Dim.HIGHER_WEIGHT);
		factories.add(HigherWeightComparator::new);
		return this;
	}

	/** Prefer placements where the box is <b>lighter</b>. */
	public PlacementComparatorBuilder lowerWeightIsBetter() {
		dims.add(Dim.LOWER_WEIGHT);
		factories.add(LowerWeightComparator::new);
		return this;
	}

	// =========================================================================
	// Support ratio  (supportedArea / (dx × dy))
	// =========================================================================

	/**
	 * Prefer placements with a <b>higher support ratio</b>.
	 *
	 * <p>Support ratio = {@code supportedArea / (dx × dy)}.  A ratio of 1.0 means the box is
	 * fully supported; lower values indicate partial overhang. Boxes of different sizes are
	 * compared by ratio (floating-point division) to avoid favouring large boxes over small ones.
	 * A box with zero footprint area is treated as the worst possible placement.
	 */
	public PlacementComparatorBuilder higherSupportIsBetter() {
		dims.add(Dim.HIGHER_SUPPORT);
		factories.add(HigherSupportComparator::new);
		return this;
	}

	/** Prefer placements with a <b>lower support ratio</b>. */
	public PlacementComparatorBuilder lowerSupportIsBetter() {
		dims.add(Dim.LOWER_SUPPORT);
		factories.add(LowerSupportComparator::new);
		return this;
	}

	// =========================================================================
	// Max-load weight limit  (maxLoadWeight on BoxStackValue)
	// =========================================================================

	/**
	 * Prefer placements where the box allows a <b>higher max-load weight</b> on top.
	 * Unconstrained boxes (no weight limit) are treated as having unlimited capacity.
	 */
	public PlacementComparatorBuilder higherMaxLoadWeightIsBetter() {
		dims.add(Dim.HIGHER_MAX_LOAD_WEIGHT);
		factories.add(HigherMaxLoadWeightComparator::new);
		return this;
	}

	/** Prefer placements where the box allows a <b>lower max-load weight</b> on top. */
	public PlacementComparatorBuilder lowerMaxLoadWeightIsBetter() {
		dims.add(Dim.LOWER_MAX_LOAD_WEIGHT);
		factories.add(LowerMaxLoadWeightComparator::new);
		return this;
	}

	// =========================================================================
	// Max-load pressure limit  (maxLoadPressure on BoxStackValue)
	// =========================================================================

	/**
	 * Prefer placements where the box tolerates a <b>higher max-load pressure</b> on top.
	 * Unconstrained boxes are treated as having unlimited pressure tolerance.
	 */
	public PlacementComparatorBuilder higherMaxLoadPressureIsBetter() {
		dims.add(Dim.HIGHER_MAX_LOAD_PRESSURE);
		factories.add(HigherMaxLoadPressureComparator::new);
		return this;
	}

	/** Prefer placements where the box tolerates a <b>lower max-load pressure</b> on top. */
	public PlacementComparatorBuilder lowerMaxLoadPressureIsBetter() {
		dims.add(Dim.LOWER_MAX_LOAD_PRESSURE);
		factories.add(LowerMaxLoadPressureComparator::new);
		return this;
	}

	// =========================================================================
	// Max-load box count limit  (maxLoadBoxCount on BoxStackValue)
	// =========================================================================

	/**
	 * Prefer placements where the box allows a <b>higher number of boxes</b> stacked on top.
	 * Unconstrained boxes are treated as having an unlimited count.
	 */
	public PlacementComparatorBuilder higherMaxLoadBoxCountIsBetter() {
		dims.add(Dim.HIGHER_MAX_LOAD_BOX_COUNT);
		factories.add(HigherMaxLoadBoxCountComparator::new);
		return this;
	}

	/** Prefer placements where the box allows a <b>lower number of boxes</b> on top. */
	public PlacementComparatorBuilder lowerMaxLoadBoxCountIsBetter() {
		dims.add(Dim.LOWER_MAX_LOAD_BOX_COUNT);
		factories.add(LowerMaxLoadBoxCountComparator::new);
		return this;
	}

	// =========================================================================
	// Identical-only restriction  (loadIdenticalBoxOnly on BoxStackValue)
	// =========================================================================

	/**
	 * Prefer placements where the box has <b>no identical-only restriction</b> — i.e. it accepts
	 * any box type on top. Fewer restrictions leave more future stacking options.
	 */
	public PlacementComparatorBuilder noIdenticalConstraintIsBetter() {
		dims.add(Dim.NO_IDENTICAL_CONSTRAINT);
		factories.add(NoIdenticalConstraintComparator::new);
		return this;
	}

	/**
	 * Prefer placements where the box <b>has</b> the identical-only restriction.
	 * This is the inverse of {@link #noIdenticalConstraintIsBetter()}.
	 */
	public PlacementComparatorBuilder identicalConstraintIsBetter() {
		dims.add(Dim.IDENTICAL_CONSTRAINT);
		factories.add(IdenticalConstraintComparator::new);
		return this;
	}

	// =========================================================================
	// Build
	// =========================================================================

	/**
	 * Builds and returns the configured comparator.
	 *
	 * <p>Scans the {@link #OPTIMIZED_REGISTRY} for the longest known dimension prefix matching
	 * the configured dimensions. When a match is found the corresponding fused comparator is
	 * used for those dimensions and the remaining suffix is chained as its {@code next}. When no
	 * prefix matches a plain per-dimension chain is produced. In both cases no {@code List} is
	 * traversed at runtime inside {@code compare()}.
	 *
	 * <p>If no dimensions have been added the returned comparator always returns 0.
	 *
	 * @return a non-null {@link PlacementComparator}
	 */
	public PlacementComparator build() {
		if (dims.isEmpty()) {
			return PlacementComparator.noOp();
		}

		// Scan registry for longest matching prefix.
		for (Map.Entry<List<Dim>, Function<PlacementComparator, AbstractChainedPlacementComparator>> entry
				: OPTIMIZED_REGISTRY) {
			List<Dim> combo = entry.getKey();
			if (dims.size() >= combo.size() && dims.subList(0, combo.size()).equals(combo)) {
				// Build plain chain for the suffix dimensions (combo.size() … end).
				PlacementComparator suffix = buildChain(combo.size(), dims.size());
				return entry.getValue().apply(suffix);
			}
		}

		return buildChain(0, dims.size());
	}

	/**
	 * Builds a plain chained comparator from {@code factories[start]} through
	 * {@code factories[end - 1]}, with the tail as {@code next = null}.
	 *
	 * @param start first factory index (inclusive)
	 * @param end   last factory index (exclusive)
	 * @return the head of the chain, or {@code null} when {@code start == end}
	 */
	private PlacementComparator buildChain(int start, int end) {
		PlacementComparator result = null;
		for (int i = end - 1; i >= start; i--) {
			result = factories.get(i).apply(result);
		}
		return result;
	}

	// =========================================================================
	// Static factory
	// =========================================================================

	/**
	 * Inspects every {@link BoxStackValue} of every item, detects active constraints and returns
	 * a comparator with the natural priority order:
	 * no-identical-restriction → higher box-count → higher weight → higher pressure.
	 *
	 * <p>When all four constraint types are present the returned comparator is the fused
	 * {@link NoIdenticalHigherCountHigherWeightHigherPressureComparator}.
	 *
	 * @param items the box items to be packed; must not be {@code null}
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	public static PlacementComparator forBoxItems(List<BoxItem> items) {
		boolean hasWeight    = false;
		boolean hasPressure  = false;
		boolean hasCount     = false;
		boolean hasIdentical = false;

		for (BoxItem item : items) {
			for (BoxStackValue sv : item.getBox().getStackValues()) {
				if (sv.isMaxLoadWeight())        hasWeight    = true;
				if (sv.isMaxLoadPressure())      hasPressure  = true;
				if (sv.isMaxLoadBoxCount())      hasCount     = true;
				if (sv.isLoadIdenticalBoxOnly()) hasIdentical = true;
			}
		}

		PlacementComparatorBuilder builder = newBuilder();
		if (hasIdentical) builder.noIdenticalConstraintIsBetter();
		if (hasCount)     builder.higherMaxLoadBoxCountIsBetter();
		if (hasWeight)    builder.higherMaxLoadWeightIsBetter();
		if (hasPressure)  builder.higherMaxLoadPressureIsBetter();
		return builder.build();
	}

	// =========================================================================
	// Inner comparator subclasses — X
	// =========================================================================

	/** Prefers lower x coordinate. */
	public static final class LowerXComparator extends AbstractChainedPlacementComparator {
		public LowerXComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher x coordinate. */
	public static final class HigherXComparator extends AbstractChainedPlacementComparator {
		public HigherXComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(a.getAbsoluteX(), b.getAbsoluteX());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — Y
	// =========================================================================

	/** Prefers lower y coordinate. */
	public static final class LowerYComparator extends AbstractChainedPlacementComparator {
		public LowerYComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteY(), a.getAbsoluteY());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher y coordinate. */
	public static final class HigherYComparator extends AbstractChainedPlacementComparator {
		public HigherYComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(a.getAbsoluteY(), b.getAbsoluteY());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — Z
	// =========================================================================

	/** Prefers lower z coordinate (floor-first). */
	public static final class LowerZComparator extends AbstractChainedPlacementComparator {
		public LowerZComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher z coordinate. */
	public static final class HigherZComparator extends AbstractChainedPlacementComparator {
		public HigherZComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(a.getAbsoluteZ(), b.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — area
	// =========================================================================

	/** Prefers larger footprint area. */
	public static final class HigherAreaComparator extends AbstractChainedPlacementComparator {
		public HigherAreaComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers smaller footprint area. */
	public static final class LowerAreaComparator extends AbstractChainedPlacementComparator {
		public LowerAreaComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(b.getStackValue().getArea(), a.getStackValue().getArea());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — volume
	// =========================================================================

	/** Prefers larger volume. */
	public static final class HigherVolumeComparator extends AbstractChainedPlacementComparator {
		public HigherVolumeComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers smaller volume. */
	public static final class LowerVolumeComparator extends AbstractChainedPlacementComparator {
		public LowerVolumeComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(b.getStackValue().getVolume(), a.getStackValue().getVolume());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — weight
	// =========================================================================

	/** Prefers heavier box. */
	public static final class HigherWeightComparator extends AbstractChainedPlacementComparator {
		public HigherWeightComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(a.getWeight(), b.getWeight());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lighter box. */
	public static final class LowerWeightComparator extends AbstractChainedPlacementComparator {
		public LowerWeightComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getWeight(), a.getWeight());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — support ratio
	// =========================================================================

	/**
	 * Prefers higher support ratio ({@code supportedArea / (dx × dy)}).
	 * A ratio of 1.0 = fully supported; lower = more overhang.
	 */
	public static final class HigherSupportComparator extends AbstractChainedPlacementComparator {
		public HigherSupportComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(a, b);
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lower support ratio. */
	public static final class LowerSupportComparator extends AbstractChainedPlacementComparator {
		public LowerSupportComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(b, a);
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — max-load weight
	// =========================================================================

	/** Prefers higher {@code maxLoadWeight}; unconstrained = {@link Long#MAX_VALUE}. */
	public static final class HigherMaxLoadWeightComparator extends AbstractChainedPlacementComparator {
		public HigherMaxLoadWeightComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			long w1 = a.getStackValue().isMaxLoadWeight() ? a.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			long w2 = b.getStackValue().isMaxLoadWeight() ? b.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			int r = Long.compare(w1, w2);
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lower {@code maxLoadWeight}; unconstrained = {@link Long#MAX_VALUE}. */
	public static final class LowerMaxLoadWeightComparator extends AbstractChainedPlacementComparator {
		public LowerMaxLoadWeightComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			long w1 = a.getStackValue().isMaxLoadWeight() ? a.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			long w2 = b.getStackValue().isMaxLoadWeight() ? b.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			int r = Long.compare(w2, w1);
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — max-load pressure
	// =========================================================================

	/** Prefers higher {@code maxLoadPressure}; unconstrained = {@link Double#MAX_VALUE}. */
	public static final class HigherMaxLoadPressureComparator extends AbstractChainedPlacementComparator {
		public HigherMaxLoadPressureComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			double p1 = a.getStackValue().isMaxLoadPressure()
					? a.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			double p2 = b.getStackValue().isMaxLoadPressure()
					? b.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			int r = Double.compare(p1, p2);
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lower {@code maxLoadPressure}; unconstrained = {@link Double#MAX_VALUE}. */
	public static final class LowerMaxLoadPressureComparator extends AbstractChainedPlacementComparator {
		public LowerMaxLoadPressureComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			double p1 = a.getStackValue().isMaxLoadPressure()
					? a.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			double p2 = b.getStackValue().isMaxLoadPressure()
					? b.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			int r = Double.compare(p2, p1);
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — max-load box count
	// =========================================================================

	/** Prefers higher {@code maxLoadBoxCount}; unconstrained = {@link Integer#MAX_VALUE}. */
	public static final class HigherMaxLoadBoxCountComparator extends AbstractChainedPlacementComparator {
		public HigherMaxLoadBoxCountComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int r = Integer.compare(c1, c2);
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lower {@code maxLoadBoxCount}; unconstrained = {@link Integer#MAX_VALUE}. */
	public static final class LowerMaxLoadBoxCountComparator extends AbstractChainedPlacementComparator {
		public LowerMaxLoadBoxCountComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int r = Integer.compare(c2, c1);
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — identical-only restriction
	// =========================================================================

	/**
	 * Prefers placements where the box has <b>no</b> identical-only restriction.
	 * Unrestricted (false) → score 1; identical-only (true) → score 0.
	 */
	public static final class NoIdenticalConstraintComparator extends AbstractChainedPlacementComparator {
		public NoIdenticalConstraintComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int i1 = a.getStackValue().isLoadIdenticalBoxOnly() ? 0 : 1;
			int i2 = b.getStackValue().isLoadIdenticalBoxOnly() ? 0 : 1;
			int r = Integer.compare(i1, i2);
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Prefers placements where the box <b>has</b> the identical-only restriction.
	 * Identical-only (true) → score 1; unrestricted (false) → score 0.
	 */
	public static final class IdenticalConstraintComparator extends AbstractChainedPlacementComparator {
		public IdenticalConstraintComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int i1 = a.getStackValue().isLoadIdenticalBoxOnly() ? 1 : 0;
			int i2 = b.getStackValue().isLoadIdenticalBoxOnly() ? 1 : 0;
			int r = Integer.compare(i1, i2);
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Support ratio helper
	// =========================================================================

	/**
	 * Computes and compares {@code supportedArea / (dx × dy)} for two placements.
	 * Returns positive when {@code a} has the higher ratio. Uses floating-point to handle
	 * boxes of different sizes fairly and to avoid integer overflow on large areas.
	 */
	private static int compareSupportRatio(Placement a, Placement b) {
		long aArea = a.getStackValue().getArea();
		long bArea = b.getStackValue().getArea();
		if (aArea == 0 && bArea == 0) return 0;
		if (aArea == 0) return -1;
		if (bArea == 0) return  1;
		double aRatio = (double) a.getSupportedArea() / aArea;
		double bRatio = (double) b.getSupportedArea() / bArea;
		return Double.compare(aRatio, bRatio);
	}

	// =========================================================================
	// Optimized fused comparators
	// =========================================================================

	/**
	 * Fused comparator for the <em>placement stability</em> preset:
	 * higher support ratio → higher volume → heavier box → lower z.
	 *
	 * <p>Registered in {@link #OPTIMIZED_REGISTRY} for the dimension sequence
	 * {@code [HIGHER_SUPPORT, HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_Z]}.
	 * Activated automatically when {@link #higherSupportHigherVolumeHigherWeightLowerZ()} (or
	 * the equivalent manual chain) is used.
	 *
	 * <p>All four comparisons are performed inside a single {@code compare()} body —
	 * no object-pointer chasing for the four dimensions that would otherwise form a
	 * four-link chain.
	 *
	 * <pre>
	 *  Side/overhead composite view — two candidate placements A and B:
	 *
	 *  ┌──────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)                  │
	 *  │                                                                  │
	 *  │  A: box 6×6, supportedArea=36 (100%)   B: box 6×6, supported=18│
	 *  │  ┌──────┐                              ┌──────┐                 │
	 *  │  │██████│  fully on shelf              │███░░░│  overhanging    │
	 *  │  └──────┘                              └──────┘                 │
	 *  │            A preferred on priority 1                            │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — volume (higher is better, breaks support ties)    │
	 *  │                                                                  │
	 *  │  A: 4×4×4 = 64        B: 2×2×2 = 8                             │
	 *  │  ┌────┐                  ┌──┐                                   │
	 *  │  │    │ vol=64           │  │ vol=8                             │
	 *  │  └────┘  ← preferred    └──┘                                   │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — weight (higher is better, breaks volume ties)     │
	 *  │                                                                  │
	 *  │  A: weight=50  ████████████    B: weight=5  ░░░░░░░░░░░░        │
	 *  │                ↑ preferred                                      │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — z coordinate (lower is better, floor-first)       │
	 *  │                                                                  │
	 *  │  Side:  z=0 ┌──┐  ← A preferred      z=5 ┌──┐  ← B            │
	 *  │         ════╧══╧══ floor                                        │
	 *  └──────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class HigherSupportHigherVolumeHigherWeightLowerZComparator
			extends AbstractChainedPlacementComparator {

		public HigherSupportHigherVolumeHigherWeightLowerZComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			// 1. Support ratio — higher is better
			int r = compareSupportRatio(a, b);
			if (r != 0) return r;
			// 2. Volume — higher is better
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			// 3. Box weight — higher is better
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			// 4. Z coordinate — lower is better
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Fused comparator for the natural constraint-chain preset:
	 * no identical restriction → higher box count → higher weight → higher pressure.
	 *
	 * <p>Registered in {@link #OPTIMIZED_REGISTRY} for the dimension sequence
	 * {@code [NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 * Activated automatically by {@link #forBoxItems(List)} when all four constraint types are
	 * present, and when the equivalent manual chain is used.
	 *
	 * <p>Unconstrained fields are treated as their respective {@code MAX_VALUE} so that
	 * unconstrained placements always outrank constrained ones.
	 *
	 * <pre>
	 *  Side view — four candidate placements with different constraint profiles:
	 *
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — identical-only restriction (no restriction preferred)  │
	 *  │                                                                      │
	 *  │  A: accepts any box   ■ ● ▲    B: identical only  ■ ■ ■             │
	 *  │              ↑ preferred (more future options)                       │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — max box count on top (higher is better)               │
	 *  │                                                                      │
	 *  │  A: limit=10   ┌──┬──┬──┐   B: limit=2  ┌──┬──┐                    │
	 *  │                 …  …  …  (up to 10)        (up to 2)                │
	 *  │                ↑ preferred                                          │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — max load weight (higher is better; ∞ = unconstrained) │
	 *  │                                                                      │
	 *  │  A: 500 kg  ░░░░░░░░░░░░░░░   B: 100 kg  ░░░░                      │
	 *  │             ↑ preferred                                             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — max load pressure (higher is better; ∞ = unconstrained│
	 *  │                                                                      │
	 *  │  A: 8.0 N/cm²  ▓▓▓▓▓▓▓▓▓    B: 2.0 N/cm²  ▓▓                      │
	 *  │                ↑ preferred                                          │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class NoIdenticalHigherCountHigherWeightHigherPressureComparator
			extends AbstractChainedPlacementComparator {

		public NoIdenticalHigherCountHigherWeightHigherPressureComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			// 1. Identical-only restriction — no restriction is preferred
			int i1 = a.getStackValue().isLoadIdenticalBoxOnly() ? 0 : 1;
			int i2 = b.getStackValue().isLoadIdenticalBoxOnly() ? 0 : 1;
			int r = Integer.compare(i1, i2);
			if (r != 0) return r;
			// 2. Max box count — higher is better (unconstrained = MAX_VALUE)
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			r = Integer.compare(c1, c2);
			if (r != 0) return r;
			// 3. Max load weight — higher is better (unconstrained = MAX_VALUE)
			long w1 = a.getStackValue().isMaxLoadWeight()
					? a.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			long w2 = b.getStackValue().isMaxLoadWeight()
					? b.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			r = Long.compare(w1, w2);
			if (r != 0) return r;
			// 4. Max load pressure — higher is better (unconstrained = MAX_VALUE)
			double p1 = a.getStackValue().isMaxLoadPressure()
					? a.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			double p2 = b.getStackValue().isMaxLoadPressure()
					? b.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			r = Double.compare(p1, p2);
			return r != 0 ? r : chain(a, b);
		}
	}
}
