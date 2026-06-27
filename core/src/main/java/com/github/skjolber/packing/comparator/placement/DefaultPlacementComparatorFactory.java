package com.github.skjolber.packing.comparator.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.comparator.AbstractChainedPlacementComparator;

/**
 * Fluent builder and {@link PlacementComparatorFactory} for {@link PlacementComparator} instances.
 *
 * <p>Each builder method appends one dimension to the comparison chain. Dimensions are evaluated
 * in the order they are added; the first non-zero result wins.
 *
 * <p>At {@link Builder#build()} time the builder scans an <em>optimized registry</em> for the
 * longest known dimension prefix. When a match is found, the corresponding fused
 * {@link AbstractChainedPlacementComparator} subclass is instantiated (handling all matched
 * dimensions in a single {@code compare()} body), with the remaining suffix dimensions chained
 * via {@link AbstractChainedPlacementComparator#linkNext}. When no prefix matches, a plain
 * per-dimension chain is produced.
 *
 * <p><b>Building a template once, using it per run</b>
 * <pre>
 * // Configure template with default registry, all 4 constraint dims + z tiebreaker:
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newFactory()
 *         .lowerZIsBetter()
 *         .compile();
 *
 * // Per-run: clone with only active constraints:
 * PlacementComparator cmp = template.newInstance(true, false, true, false);
 * // → produces: higherCount → higherWeight → lowerZ
 * </pre>
 *
 * <p><b>Adding a custom fused comparator to the registry</b>
 * <pre>
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newBuilder()
 *         .withPositionDimensions(b -&gt; b.higherVolumeIsBetter().lowerZIsBetter())
 *         .addRegistryEntry(
 *                 List.of(HIGHER_VOLUME, LOWER_Z),
 *                 MyVolumeZComparator::new)
 *         .compile();
 * </pre>
 *
 * <p><b>Using the builder pattern for a configurable template</b>
 * <pre>
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newBuilder()
 *         .withPressureEnabled(false)
 *         .withPositionDimensions(b -&gt; b.lowerZIsBetter().higherVolumeIsBetter())
 *         .compile();
 *
 * // Per-run:
 * PlacementComparator cmp = template.create(true, true, true, false);
 * </pre>
 */
public final class DefaultPlacementComparatorFactory implements PlacementComparatorFactory {

	// =========================================================================
	// RegistryEntry — one optimized prefix in the registry
	// =========================================================================

	/**
	 * Pairs an ordered list of {@link PlacementComparatorAttribute}s (the prefix to match)
	 * with a supplier that creates a single fused {@link AbstractChainedPlacementComparator}
	 * handling all those attributes in one {@code compare()} body.
	 *
	 * <p>Use {@link #registryEntry} to create instances conveniently.
	 *
	 * <pre>
	 *  // Example: register a fused "higher-volume then lower-z" comparator
	 *  RegistryEntry e = DefaultPlacementComparatorFactory.registryEntry(
	 *          List.of(HIGHER_VOLUME, LOWER_Z),
	 *          MyVolumeZComparator::new);
	 * </pre>
	 */
	public record RegistryEntry(
			List<PlacementComparatorAttribute> attributes,
			PlacementComparatorSupplier supplier) {}

	// =========================================================================
	// AttrEntry — one configured dimension
	// =========================================================================

	/**
	 * Pairs a {@link PlacementComparatorAttribute} (used for filtering and registry lookup)
	 * with the supplier that creates the comparator node.
	 */
	private record AttrEntry(
			PlacementComparatorAttribute attribute,
			PlacementComparatorSupplier supplier) {}

	// =========================================================================
	// Default optimized registry
	// =========================================================================

	/** Built-in registry, computed once. Contains the two default fused presets. */
	private static final List<RegistryEntry> DEFAULT_REGISTRY;

	static {
		List<RegistryEntry> reg = new ArrayList<>();

		// high support → high volume → high weight → low z  (placement stability preset)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT, PlacementComparatorAttribute.LOWER_Z),
				HigherSupportHigherVolumeHigherWeightLowerZComparator::new));

		// no-identical → high count → high weight → high pressure  (all-constraint fused)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				NoIdenticalHigherCountHigherWeightHigherPressureComparator::new));

		reg.sort((e1, e2) -> Integer.compare(e2.attributes().size(), e1.attributes().size()));
		DEFAULT_REGISTRY = Collections.unmodifiableList(reg);
	}

	// =========================================================================
	// Immutable instance fields (compiled factory)
	// =========================================================================

	/** Ordered comparison dimensions for this compiled factory. */
	private final List<AttrEntry> entries;

	/** Per-instance registry used for optimized prefix matching during {@link #build()}. */
	private final List<RegistryEntry> optimizedRegistry;

	// =========================================================================
	// Package-private constructor (used by Builder.compile())
	// =========================================================================

	DefaultPlacementComparatorFactory(List<AttrEntry> entries, List<RegistryEntry> registry) {
		List<RegistryEntry> sorted = new ArrayList<>(registry);
		sorted.sort((e1, e2) -> Integer.compare(e2.attributes().size(), e1.attributes().size()));
		this.optimizedRegistry = Collections.unmodifiableList(sorted);
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
	}

	// =========================================================================
	// Static factory methods (return Builder)
	// =========================================================================

	/**
	 * Returns a builder pre-loaded with all four load-constraint entries in natural priority
	 * order: identical → count → weight → pressure.
	 *
	 * <p>Position / size dimensions (z, area, volume, …) are <em>not</em> pre-loaded; add them
	 * after construction as tiebreakers.
	 *
	 * <p>Call {@link Builder#compile()} to obtain an immutable {@link DefaultPlacementComparatorFactory},
	 * or call {@link Builder#build()} to obtain a {@link PlacementComparator} directly.
	 */
	public static Builder newFactory() {
		Builder b = new Builder();
		b.entries.add(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
				NoIdenticalConstraintComparator::new));
		b.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
				HigherMaxLoadBoxCountComparator::new));
		b.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
				HigherMaxLoadWeightComparator::new));
		b.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
				HigherMaxLoadPressureComparator::new));
		return b;
	}

	/**
	 * Returns a builder pre-loaded with all four load-constraint entries; equivalent to
	 * {@link #newFactory()}. Supports {@link Builder#withWeightEnabled(boolean)} and other
	 * enable flags, plus {@link Builder#withPositionDimensions(Consumer)}.
	 */
	public static Builder newBuilder() {
		return newFactory();
	}

	/**
	 * Returns an empty builder with the default registry and no pre-loaded entries.
	 * Use for position-only comparators or when full manual configuration is preferred.
	 */
	public static Builder emptyFactory() {
		return new Builder();
	}

	// =========================================================================
	// Named presets
	// =========================================================================

	/**
	 * Returns a builder pre-configured with the <em>placement stability</em> strategy:
	 * higher support ratio → higher volume → heavier box → lower z.
	 *
	 * <p>Always resolves to {@link HigherSupportHigherVolumeHigherWeightLowerZComparator}.
	 *
	 * @return a builder with the four stability dimensions pre-added (no constraint dims)
	 */
	public static Builder higherSupportHigherVolumeHigherWeightLowerZ() {
		return emptyFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerZIsBetter();
	}

	// =========================================================================
	// Static registry helpers
	// =========================================================================

	/**
	 * Returns the built-in default registry.
	 * Contains the two built-in fused presets: placement stability and all-constraints.
	 * Useful as a base when building a custom registry that extends the defaults.
	 *
	 * @return an unmodifiable copy of the default registry
	 */
	public static List<RegistryEntry> defaultRegistry() {
		return DEFAULT_REGISTRY;
	}

	/**
	 * Convenience factory for a {@link RegistryEntry}.
	 *
	 * <pre>
	 *  RegistryEntry e = DefaultPlacementComparatorFactory.registryEntry(
	 *          List.of(HIGHER_VOLUME, LOWER_Z),
	 *          MyVolumeZComparator::new);
	 * </pre>
	 *
	 * @param attributes ordered list of attributes that form the match prefix
	 * @param supplier   constructor reference or lambda that creates the fused comparator
	 * @return a new registry entry
	 */
	public static RegistryEntry registryEntry(
			List<PlacementComparatorAttribute> attributes,
			PlacementComparatorSupplier supplier) {
		return new RegistryEntry(attributes, supplier);
	}

	// =========================================================================
	// Static factory for box items
	// =========================================================================

	/**
	 * Inspects every {@link BoxStackValue} of every item, detects active constraints and returns
	 * a comparator with the natural priority order:
	 * no-identical-restriction → higher box-count → higher weight → higher pressure.
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

		return newFactory()
				.withConstraints(hasWeight, hasPressure, hasCount, hasIdentical)
				.build();
	}

	// =========================================================================
	// PlacementComparatorFactory implementation
	// =========================================================================

	/**
	 * Builds and returns the configured comparator, skipping any entry whose attribute appears
	 * in {@code disabled}.
	 *
	 * <p>Scans the per-instance {@link #optimizedRegistry} for the longest matching attribute
	 * prefix in the active entry list. When a match is found the corresponding fused comparator
	 * is used; the remaining suffix is chained via {@link AbstractChainedPlacementComparator#linkNext}.
	 * When no prefix matches a plain per-dimension chain is produced.
	 *
	 * @param disabled attributes to skip; must not be {@code null}
	 * @return a non-null {@link PlacementComparator}; the no-op comparator if no active entries
	 */
	@Override
	public PlacementComparator build(Collection<PlacementComparatorAttribute> disabled) {
		List<AttrEntry> active = disabled.isEmpty() ? entries : filtered(disabled);
		return buildFrom(active);
	}

	// =========================================================================
	// Clone with constraint parameters
	// =========================================================================

	/**
	 * Returns a new compiled factory containing only the constraint entries whose type is active,
	 * plus all position / size entries (which are never filtered), in their original order.
	 * The clone inherits this factory's {@link #optimizedRegistry}.
	 *
	 * @param weight    include weight-constraint entry if present
	 * @param pressure  include pressure-constraint entry if present
	 * @param count     include count-constraint entry if present
	 * @param identical include identical-only-constraint entry if present
	 * @return a new immutable {@link DefaultPlacementComparatorFactory}
	 */
	public DefaultPlacementComparatorFactory withConstraints(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = buildAvailableSet(weight, pressure, count, identical);
		List<AttrEntry> filtered = new ArrayList<>();
		for (AttrEntry e : entries) {
			if (!e.attribute().isSkippable(available)) {
				filtered.add(e);
			}
		}
		return new DefaultPlacementComparatorFactory(filtered, optimizedRegistry);
	}

	/**
	 * Returns a {@link Builder} pre-loaded with entries filtered for the given constraint profile.
	 * The caller may append additional dimensions before calling {@link Builder#build()}.
	 *
	 * @param weight    include weight entry if present in this factory
	 * @param pressure  include pressure entry if present
	 * @param count     include count entry if present
	 * @param identical include identical entry if present
	 * @return a mutable {@link Builder} initialized from this factory's filtered entries
	 */
	public Builder createBuilder(boolean weight, boolean pressure, boolean count, boolean identical) {
		Set<String> available = buildAvailableSet(weight, pressure, count, identical);
		Builder b = new Builder();
		b.registry = new ArrayList<>(optimizedRegistry);
		for (AttrEntry e : entries) {
			if (!e.attribute().isSkippable(available)) {
				b.entries.add(e);
			}
		}
		return b;
	}

	/**
	 * Convenience method — builds a {@link PlacementComparator} for the given constraint profile.
	 * Equivalent to {@code withConstraints(...).build()}.
	 *
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	public PlacementComparator create(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		return withConstraints(weight, pressure, count, identical).build();
	}

	/**
	 * Produces a comparator for the given constraint profile by cloning the template via
	 * {@link #withConstraints(boolean, boolean, boolean, boolean)} and calling {@link #build()}.
	 *
	 * @param weight    whether the weight constraint is active
	 * @param pressure  whether the pressure constraint is active
	 * @param count     whether the box-count constraint is active
	 * @param identical whether the identical-only constraint is active
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	public PlacementComparator newInstance(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		return withConstraints(weight, pressure, count, identical).build();
	}

	/**
	 * Returns a new factory with the matching attribute entries removed.
	 * The current instance is not modified.
	 *
	 * @param attribute the attribute whose entries to remove; must not be {@code null}
	 * @return a new immutable factory without the matching entries
	 */
	public DefaultPlacementComparatorFactory withoutAttribute(PlacementComparatorAttribute attribute) {
		List<AttrEntry> filtered = new ArrayList<>(entries);
		filtered.removeIf(e -> e.attribute().getId().equals(attribute.getId()));
		return new DefaultPlacementComparatorFactory(filtered, optimizedRegistry);
	}

	/**
	 * Returns a new factory with the same entries and registry as this one, but with an
	 * additional registry entry appended. The current instance is not modified.
	 *
	 * @param attributes ordered list of attributes that form the match prefix
	 * @param supplier   constructor reference or lambda that creates the fused comparator
	 * @return a new factory with the extended registry
	 */
	public DefaultPlacementComparatorFactory withAddedRegistryEntry(
			List<PlacementComparatorAttribute> attributes,
			PlacementComparatorSupplier supplier) {
		List<RegistryEntry> extended = new ArrayList<>(optimizedRegistry);
		extended.add(new RegistryEntry(attributes, supplier));
		return new DefaultPlacementComparatorFactory(new ArrayList<>(entries), extended);
	}

	// =========================================================================
	// Build internals
	// =========================================================================

	private List<AttrEntry> filtered(Collection<PlacementComparatorAttribute> disabled) {
		Set<String> disabledIds = new HashSet<>(disabled.size() * 2);
		for (PlacementComparatorAttribute a : disabled) disabledIds.add(a.getId());
		List<AttrEntry> active = new ArrayList<>(entries.size());
		for (AttrEntry e : entries) {
			if (!disabledIds.contains(e.attribute().getId())) active.add(e);
		}
		return active;
	}

	private PlacementComparator buildFrom(List<AttrEntry> active) {
		if (active.isEmpty()) {
			return PlacementComparator.noOp();
		}

		List<PlacementComparatorAttribute> attrs = new ArrayList<>(active.size());
		List<PlacementComparatorSupplier> suppliers = new ArrayList<>(active.size());
		for (AttrEntry e : active) {
			attrs.add(e.attribute());
			suppliers.add(e.supplier());
		}

		for (RegistryEntry entry : optimizedRegistry) {
			List<PlacementComparatorAttribute> combo = entry.attributes();
			if (attrs.size() >= combo.size() && attrs.subList(0, combo.size()).equals(combo)) {
				PlacementComparator suffix = buildChain(suppliers, combo.size(), attrs.size());
				PlacementComparator head = entry.supplier().get();
				return link(head, suffix);
			}
		}

		return buildChain(suppliers, 0, attrs.size());
	}

	private static PlacementComparator buildChain(
			List<PlacementComparatorSupplier> suppliers, int start, int end) {
		PlacementComparator result = null;
		for (int i = end - 1; i >= start; i--) {
			PlacementComparator comp = suppliers.get(i).get();
			result = link(comp, result);
		}
		return result;
	}

	private static PlacementComparator link(PlacementComparator comparator, PlacementComparator suffix) {
		if (suffix == null) return comparator;
		if (comparator instanceof AbstractChainedPlacementComparator acc) {
			acc.linkNext(suffix);
			return comparator;
		}
		// Fallback for plain PlacementComparator implementations
		return (a, b) -> {
			int r = comparator.compare(a, b);
			return r != 0 ? r : suffix.compare(a, b);
		};
	}

	private static Set<String> buildAvailableSet(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = new HashSet<>(4);
		if (weight)    available.add("weight");
		if (pressure)  available.add("pressure");
		if (count)     available.add("count");
		if (identical) available.add("identical");
		return available;
	}

	// =========================================================================
	// Builder inner class
	// =========================================================================

	/**
	 * Mutable builder that configures which comparison dimensions are included.
	 *
	 * <p>Returned by {@link #newFactory()}, {@link #newBuilder()}, and {@link #emptyFactory()}.
	 * Also returned by {@link DefaultPlacementComparatorFactory#createBuilder(boolean, boolean, boolean, boolean)}.
	 *
	 * <p>Call {@link #compile()} to obtain an immutable {@link DefaultPlacementComparatorFactory}
	 * that can be used repeatedly as a template (calling {@code create()} or {@code createBuilder()}
	 * per packing run). Call {@link #build()} to obtain a {@link PlacementComparator} directly.
	 *
	 * <p>By default the builder uses the {@link #defaultRegistry() default registry}. To extend
	 * it with custom fused comparators use {@link #addRegistryEntry}; to replace it entirely
	 * use {@link #withRegistry}.
	 */
	public static final class Builder implements PlacementComparatorFactory {

		private final List<AttrEntry> entries = new ArrayList<>();
		/** {@code null} means "use DEFAULT_REGISTRY"; non-null is a custom/extended list. */
		private List<RegistryEntry> registry;
		/** Applied to a temp builder at {@link #compile()} time to add position/size entries. */
		private Consumer<Builder> positionConfigurer;

		private Builder() {}

		// =========================================================================
		// Constraint enable/disable flags
		// =========================================================================

		/**
		 * Controls whether the max-load weight dimension is present in the template.
		 * When {@code false} the weight entry (if pre-loaded) is permanently removed.
		 * Default: {@code true} (no change).
		 */
		public Builder withWeightEnabled(boolean enabled) {
			if (!enabled) withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT);
			return this;
		}

		/** Controls whether the max-load pressure dimension is present. Default: {@code true}. */
		public Builder withPressureEnabled(boolean enabled) {
			if (!enabled) withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE);
			return this;
		}

		/** Controls whether the max-load box count dimension is present. Default: {@code true}. */
		public Builder withCountEnabled(boolean enabled) {
			if (!enabled) withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT);
			return this;
		}

		/** Controls whether the identical-only restriction dimension is present. Default: {@code true}. */
		public Builder withIdenticalEnabled(boolean enabled) {
			if (!enabled) withoutAttribute(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT);
			return this;
		}

		// =========================================================================
		// X coordinate
		// =========================================================================

		/** Prefer placements with a <b>lower x</b> coordinate. */
		public Builder lowerXIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_X, LowerXComparator::new));
			return this;
		}

		/** Prefer placements with a <b>higher x</b> coordinate. */
		public Builder higherXIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_X, HigherXComparator::new));
			return this;
		}

		// =========================================================================
		// Y coordinate
		// =========================================================================

		/** Prefer placements with a <b>lower y</b> coordinate. */
		public Builder lowerYIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_Y, LowerYComparator::new));
			return this;
		}

		/** Prefer placements with a <b>higher y</b> coordinate. */
		public Builder higherYIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_Y, HigherYComparator::new));
			return this;
		}

		// =========================================================================
		// Z coordinate
		// =========================================================================

		/** Prefer placements with a <b>lower z</b> coordinate (floor-first strategy). */
		public Builder lowerZIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_Z, LowerZComparator::new));
			return this;
		}

		/** Prefer placements with a <b>higher z</b> coordinate. */
		public Builder higherZIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_Z, HigherZComparator::new));
			return this;
		}

		// =========================================================================
		// Footprint area  (dx × dy)
		// =========================================================================

		/** Prefer placements where the box has a <b>larger footprint area</b>. */
		public Builder higherAreaIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_AREA, HigherAreaComparator::new));
			return this;
		}

		/** Prefer placements where the box has a <b>smaller footprint area</b>. */
		public Builder lowerAreaIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_AREA, LowerAreaComparator::new));
			return this;
		}

		// =========================================================================
		// Volume  (dx × dy × dz)
		// =========================================================================

		/** Prefer placements where the box has a <b>larger volume</b>. */
		public Builder higherVolumeIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_VOLUME, HigherVolumeComparator::new));
			return this;
		}

		/** Prefer placements where the box has a <b>smaller volume</b>. */
		public Builder lowerVolumeIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_VOLUME, LowerVolumeComparator::new));
			return this;
		}

		// =========================================================================
		// Box weight (physical weight of the box)
		// =========================================================================

		/** Prefer placements where the box is <b>heavier</b>. */
		public Builder higherWeightIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_WEIGHT, HigherWeightComparator::new));
			return this;
		}

		/** Prefer placements where the box is <b>lighter</b>. */
		public Builder lowerWeightIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_WEIGHT, LowerWeightComparator::new));
			return this;
		}

		// =========================================================================
		// Support ratio  (supportedArea / (dx × dy))
		// =========================================================================

		/**
		 * Prefer placements with a <b>higher support ratio</b>.
		 * Ratio = {@code supportedArea / (dx × dy)}. 1.0 = fully supported; lower = more overhang.
		 */
		public Builder higherSupportIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_SUPPORT, HigherSupportComparator::new));
			return this;
		}

		/** Prefer placements with a <b>lower support ratio</b>. */
		public Builder lowerSupportIsBetter() {
			entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_SUPPORT, LowerSupportComparator::new));
			return this;
		}

		// =========================================================================
		// Max-load weight limit  (replaceOrAdd semantics)
		// =========================================================================

		/** Prefer placements where the box allows a <b>higher max-load weight</b> on top. Unconstrained = unlimited. */
		public Builder higherMaxLoadWeightIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
					HigherMaxLoadWeightComparator::new));
			return this;
		}

		/** Prefer placements where the box allows a <b>lower max-load weight</b> on top. */
		public Builder lowerMaxLoadWeightIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_WEIGHT,
					LowerMaxLoadWeightComparator::new));
			return this;
		}

		// =========================================================================
		// Max-load pressure limit
		// =========================================================================

		/** Prefer placements where the box tolerates a <b>higher max-load pressure</b> on top. */
		public Builder higherMaxLoadPressureIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
					HigherMaxLoadPressureComparator::new));
			return this;
		}

		/** Prefer placements where the box tolerates a <b>lower max-load pressure</b> on top. */
		public Builder lowerMaxLoadPressureIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_PRESSURE,
					LowerMaxLoadPressureComparator::new));
			return this;
		}

		// =========================================================================
		// Max-load box count limit
		// =========================================================================

		/** Prefer placements where the box allows a <b>higher number of boxes</b> stacked on top. */
		public Builder higherMaxLoadBoxCountIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
					HigherMaxLoadBoxCountComparator::new));
			return this;
		}

		/** Prefer placements where the box allows a <b>lower number of boxes</b> on top. */
		public Builder lowerMaxLoadBoxCountIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_BOX_COUNT,
					LowerMaxLoadBoxCountComparator::new));
			return this;
		}

		// =========================================================================
		// Identical-only restriction
		// =========================================================================

		/**
		 * Prefer placements where the box has <b>no identical-only restriction</b>.
		 * Fewer restrictions leave more future stacking options.
		 */
		public Builder noIdenticalConstraintIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
					NoIdenticalConstraintComparator::new));
			return this;
		}

		/** Prefer placements where the box <b>has</b> the identical-only restriction. */
		public Builder identicalConstraintIsBetter() {
			replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.IDENTICAL_CONSTRAINT,
					IdenticalConstraintComparator::new));
			return this;
		}

		// =========================================================================
		// Clone with constraint parameters
		// =========================================================================

		/**
		 * Returns a new builder containing only the constraint entries whose type is active,
		 * plus all position / size entries (which are never filtered), in their original order.
		 *
		 * @param weight    include weight-constraint entry if present
		 * @param pressure  include pressure-constraint entry if present
		 * @param count     include count-constraint entry if present
		 * @param identical include identical-only-constraint entry if present
		 * @return a new, independently mutable {@link Builder}
		 */
		public Builder withConstraints(boolean weight, boolean pressure,
				boolean count, boolean identical) {
			Set<String> available = buildAvailableSet(weight, pressure, count, identical);
			Builder clone = new Builder();
			clone.registry = this.registry;
			for (AttrEntry e : entries) {
				if (!e.attribute().isSkippable(available)) {
					clone.entries.add(e);
				}
			}
			return clone;
		}

		/**
		 * Removes all entries whose attribute ID matches that of the given attribute, in-place.
		 * Returns {@code this}.
		 *
		 * @param attribute the attribute whose entries to remove; must not be {@code null}
		 * @return {@code this}
		 */
		public Builder withoutAttribute(PlacementComparatorAttribute attribute) {
			entries.removeIf(e -> e.attribute().getId().equals(attribute.getId()));
			return this;
		}

		// =========================================================================
		// Registry configuration
		// =========================================================================

		/**
		 * Replaces the entire optimized registry with the supplied entries.
		 *
		 * @param registry the registry to use; {@code null} or empty to use an empty registry
		 * @return {@code this}
		 */
		public Builder withRegistry(List<RegistryEntry> registry) {
			this.registry = registry != null ? new ArrayList<>(registry) : new ArrayList<>();
			return this;
		}

		/**
		 * Appends a single entry to the registry, extending whichever registry is currently set
		 * (default or custom). The first call initialises from the default registry.
		 *
		 * @param attributes ordered list of attributes that form the match prefix
		 * @param supplier   constructor reference or lambda that creates the fused comparator
		 * @return {@code this}
		 */
		public Builder addRegistryEntry(List<PlacementComparatorAttribute> attributes,
				PlacementComparatorSupplier supplier) {
			if (registry == null) {
				registry = new ArrayList<>(DEFAULT_REGISTRY);
			}
			registry.add(new RegistryEntry(attributes, supplier));
			return this;
		}

		/**
		 * Resets the registry to the built-in {@link #defaultRegistry() default}.
		 *
		 * @return {@code this}
		 */
		public Builder withDefaultRegistry() {
			this.registry = null;
			return this;
		}

		/**
		 * Sets a configurer that appends position / size dimensions (e.g. lower-z, higher-volume)
		 * to the template after all constraint dimensions. Applied at {@link #compile()} time.
		 *
		 * @param configurer consumer that adds position dims to a temp builder; {@code null} to clear
		 * @return {@code this}
		 */
		public Builder withPositionDimensions(Consumer<Builder> configurer) {
			this.positionConfigurer = configurer;
			return this;
		}

		// =========================================================================
		// Build methods
		// =========================================================================

		/**
		 * Builds and returns an immutable {@link DefaultPlacementComparatorFactory} from
		 * the current configuration. The positionConfigurer (if set) is applied to a temporary
		 * builder and its entries are appended after the constraint entries.
		 *
		 * @return a compiled, immutable factory
		 */
		public DefaultPlacementComparatorFactory compile() {
			List<AttrEntry> toCompile = new ArrayList<>(entries);
			if (positionConfigurer != null) {
				Builder temp = new Builder();
				positionConfigurer.accept(temp);
				toCompile.addAll(temp.entries);
			}
			List<RegistryEntry> reg = registry != null ? registry : DEFAULT_REGISTRY;
			return new DefaultPlacementComparatorFactory(toCompile, reg);
		}

		/**
		 * Builds a {@link PlacementComparator} from this builder's current state,
		 * skipping any entry whose attribute appears in {@code disabled}.
		 * Equivalent to {@code compile().build(disabled)}.
		 */
		@Override
		public PlacementComparator build(Collection<PlacementComparatorAttribute> disabled) {
			return compile().build(disabled);
		}

		private void replaceOrAdd(AttrEntry entry) {
			String id = entry.attribute().getId();
			for (int i = 0; i < entries.size(); i++) {
				if (entries.get(i).attribute().getId().equals(id)) {
					entries.set(i, entry);
					return;
				}
			}
			entries.add(entry);
		}
	}

	// =========================================================================
	// Inner comparator subclasses — X
	// =========================================================================

	/** Prefers lower x coordinate. */
	public static final class LowerXComparator extends AbstractChainedPlacementComparator {
		public LowerXComparator() {}
		public LowerXComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher x coordinate. */
	public static final class HigherXComparator extends AbstractChainedPlacementComparator {
		public HigherXComparator() {}
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
		public LowerYComparator() {}
		public LowerYComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteY(), a.getAbsoluteY());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher y coordinate. */
	public static final class HigherYComparator extends AbstractChainedPlacementComparator {
		public HigherYComparator() {}
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
		public LowerZComparator() {}
		public LowerZComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers higher z coordinate. */
	public static final class HigherZComparator extends AbstractChainedPlacementComparator {
		public HigherZComparator() {}
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
		public HigherAreaComparator() {}
		public HigherAreaComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers smaller footprint area. */
	public static final class LowerAreaComparator extends AbstractChainedPlacementComparator {
		public LowerAreaComparator() {}
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
		public HigherVolumeComparator() {}
		public HigherVolumeComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers smaller volume. */
	public static final class LowerVolumeComparator extends AbstractChainedPlacementComparator {
		public LowerVolumeComparator() {}
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
		public HigherWeightComparator() {}
		public HigherWeightComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(a.getWeight(), b.getWeight());
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lighter box. */
	public static final class LowerWeightComparator extends AbstractChainedPlacementComparator {
		public LowerWeightComparator() {}
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

	/** Prefers higher support ratio ({@code supportedArea / (dx × dy)}). */
	public static final class HigherSupportComparator extends AbstractChainedPlacementComparator {
		public HigherSupportComparator() {}
		public HigherSupportComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(a, b);
			return r != 0 ? r : chain(a, b);
		}
	}

	/** Prefers lower support ratio. */
	public static final class LowerSupportComparator extends AbstractChainedPlacementComparator {
		public LowerSupportComparator() {}
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
		public HigherMaxLoadWeightComparator() {}
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
		public LowerMaxLoadWeightComparator() {}
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
		public HigherMaxLoadPressureComparator() {}
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
		public LowerMaxLoadPressureComparator() {}
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
		public HigherMaxLoadBoxCountComparator() {}
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
		public LowerMaxLoadBoxCountComparator() {}
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

	/** Prefers placements where the box has <b>no</b> identical-only restriction. */
	public static final class NoIdenticalConstraintComparator extends AbstractChainedPlacementComparator {
		public NoIdenticalConstraintComparator() {}
		public NoIdenticalConstraintComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;  // absence is better → no-constraint wins
			return chain(a, b);
		}
	}

	/** Prefers placements where the box <b>has</b> the identical-only restriction. */
	public static final class IdenticalConstraintComparator extends AbstractChainedPlacementComparator {
		public IdenticalConstraintComparator() {}
		public IdenticalConstraintComparator(PlacementComparator next) { super(next); }

		@Override
		public int compare(Placement a, Placement b) {
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? 1 : -1;  // presence is better → constrained wins
			return chain(a, b);
		}
	}

	// =========================================================================
	// Support ratio helper
	// =========================================================================

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
	 * <p>Registered in {@link #DEFAULT_REGISTRY} for the attribute sequence
	 * {@code [HIGHER_SUPPORT, HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_Z]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)                  │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — volume (higher is better)                         │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — box weight (higher is better)                     │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — z coordinate (lower is better, floor-first)       │
	 *  └──────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class HigherSupportHigherVolumeHigherWeightLowerZComparator
			extends AbstractChainedPlacementComparator {

		public HigherSupportHigherVolumeHigherWeightLowerZComparator() {}

		public HigherSupportHigherVolumeHigherWeightLowerZComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(a, b);
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Fused comparator for the natural constraint-chain preset:
	 * no identical restriction → higher box count → higher weight → higher pressure.
	 *
	 * <p>Registered in {@link #DEFAULT_REGISTRY} for the attribute sequence
	 * {@code [NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — identical-only restriction (no restriction preferred)  │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — max box count on top (higher is better)               │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — max load weight (higher is better; ∞ = unconstrained) │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — max load pressure (higher is better)                  │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class NoIdenticalHigherCountHigherWeightHigherPressureComparator
			extends AbstractChainedPlacementComparator {

		public NoIdenticalHigherCountHigherWeightHigherPressureComparator() {}

		public NoIdenticalHigherCountHigherWeightHigherPressureComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;  // absence is better
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int r = Integer.compare(c1, c2);
			if (r != 0) return r;
			long w1 = a.getStackValue().isMaxLoadWeight()
					? a.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			long w2 = b.getStackValue().isMaxLoadWeight()
					? b.getStackValue().getMaxLoadWeight() : Long.MAX_VALUE;
			r = Long.compare(w1, w2);
			if (r != 0) return r;
			double p1 = a.getStackValue().isMaxLoadPressure()
					? a.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			double p2 = b.getStackValue().isMaxLoadPressure()
					? b.getStackValue().getMaxLoadPressure() : Double.MAX_VALUE;
			r = Double.compare(p1, p2);
			return r != 0 ? r : chain(a, b);
		}
	}
}
