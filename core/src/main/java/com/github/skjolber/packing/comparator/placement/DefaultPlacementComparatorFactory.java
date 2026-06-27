package com.github.skjolber.packing.comparator.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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
 * <p>At {@link #build()} time the builder scans an <em>optimized registry</em> for the
 * longest known dimension prefix. When a match is found, the corresponding fused
 * {@link AbstractChainedPlacementComparator} subclass is instantiated (handling all matched
 * dimensions in a single {@code compare()} body), with the remaining suffix dimensions chained
 * as its {@code next}. When no prefix matches, a plain per-dimension chain is produced.
 *
 * <p>The optimized registry is an instance field, configurable via constructor or
 * {@link Builder#addRegistryEntry}/{@link Builder#withRegistry}. The
 * {@linkplain #DefaultPlacementComparatorFactory() no-arg constructor} uses the built-in
 * default registry (see {@link #defaultRegistry()}). A fully custom registry can be supplied via
 * {@link #DefaultPlacementComparatorFactory(List)}.
 *
 * <p><b>Building a template once, using it per run</b>
 * <pre>
 * // Configure template with default registry, all 4 constraint dims + z tiebreaker:
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newFactory()
 *         .lowerZIsBetter();
 *
 * // Per-run: clone with only active constraints:
 * PlacementComparator cmp = template.newInstance(true, false, true, false);
 * // → produces: higherCount → higherWeight → lowerZ
 * </pre>
 *
 * <p><b>Adding a custom fused comparator to the registry</b>
 * <pre>
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newBuilder()
 *         .withPositionDimensions(f -&gt; f.higherVolumeIsBetter().lowerZIsBetter())
 *         .addRegistryEntry(
 *                 List.of(HIGHER_VOLUME, LOWER_Z),
 *                 MyVolumeZComparator::new)
 *         .build();
 * </pre>
 *
 * <p><b>Using the builder pattern for a configurable template</b>
 * <pre>
 * DefaultPlacementComparatorFactory template = DefaultPlacementComparatorFactory.newBuilder()
 *         .withPressureEnabled(false)
 *         .withPositionDimensions(f -&gt; f.lowerZIsBetter().higherVolumeIsBetter())
 *         .build();
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
	 * with a factory that creates a single fused {@link AbstractChainedPlacementComparator}
	 * handling all those attributes in one {@code compare()} body.
	 *
	 * <p>Use {@link #registryEntry} to create instances without the verbose generic types.
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
			Function<PlacementComparator, AbstractChainedPlacementComparator> factory) {}

	// =========================================================================
	// AttrEntry — one configured dimension
	// =========================================================================

	/**
	 * Pairs a {@link PlacementComparatorAttribute} (used for filtering and registry lookup)
	 * with the factory that creates the comparator node.
	 */
	private record AttrEntry(
			PlacementComparatorAttribute attribute,
			Function<PlacementComparator, AbstractChainedPlacementComparator> factory) {}

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
	// Instance fields
	// =========================================================================

	/** Per-instance registry used for optimized prefix matching during {@link #build()}. */
	private final List<RegistryEntry> optimizedRegistry;

	/** Accumulated comparison dimensions, in priority order. */
	private final List<AttrEntry> entries = new ArrayList<>();

	// =========================================================================
	// Constructors
	// =========================================================================

	/** Creates an empty factory with the default optimized registry. */
	public DefaultPlacementComparatorFactory() {
		this.optimizedRegistry = DEFAULT_REGISTRY;
	}

	/**
	 * Creates an empty factory with a custom optimized registry.
	 * The registry entries are sorted longest-first at factory creation time.
	 *
	 * @param optimizedRegistry custom registry of fused comparator presets; may be empty
	 */
	public DefaultPlacementComparatorFactory(List<RegistryEntry> optimizedRegistry) {
		List<RegistryEntry> sorted = new ArrayList<>(optimizedRegistry);
		sorted.sort((e1, e2) -> Integer.compare(e2.attributes().size(), e1.attributes().size()));
		this.optimizedRegistry = Collections.unmodifiableList(sorted);
	}

	// =========================================================================
	// Static factory methods
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
	 * Avoids the need to spell out the full generic types.
	 *
	 * <pre>
	 *  RegistryEntry e = DefaultPlacementComparatorFactory.registryEntry(
	 *          List.of(HIGHER_VOLUME, LOWER_Z),
	 *          MyVolumeZComparator::new);
	 * </pre>
	 *
	 * @param attributes ordered list of attributes that form the match prefix
	 * @param factory    constructor reference or lambda that creates the fused comparator
	 * @return a new registry entry
	 */
	public static RegistryEntry registryEntry(
			List<PlacementComparatorAttribute> attributes,
			Function<PlacementComparator, AbstractChainedPlacementComparator> factory) {
		return new RegistryEntry(attributes, factory);
	}

	/**
	 * Returns a factory pre-loaded with all four load-constraint entries in natural priority
	 * order: identical → count → weight → pressure.
	 *
	 * <p>Position / size dimensions (z, area, volume, …) are <em>not</em> pre-loaded; add them
	 * after construction as tiebreakers.
	 *
	 * <p>Use {@link #newInstance(boolean, boolean, boolean, boolean)} to produce a comparator
	 * for a specific constraint profile, or {@link #withConstraints(boolean, boolean, boolean, boolean)}
	 * to clone the factory with only the active entries.
	 */
	public static DefaultPlacementComparatorFactory newFactory() {
		DefaultPlacementComparatorFactory f = new DefaultPlacementComparatorFactory();
		f.entries.add(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
				NoIdenticalConstraintComparator::new));
		f.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
				HigherMaxLoadBoxCountComparator::new));
		f.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
				HigherMaxLoadWeightComparator::new));
		f.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
				HigherMaxLoadPressureComparator::new));
		return f;
	}

	/**
	 * Returns an empty factory with the default registry and no pre-loaded entries.
	 * Use for position-only comparators or when full manual configuration is preferred.
	 */
	public static DefaultPlacementComparatorFactory emptyFactory() {
		return new DefaultPlacementComparatorFactory();
	}

	// =========================================================================
	// Named presets
	// =========================================================================

	/**
	 * Returns a factory pre-configured with the <em>placement stability</em> strategy:
	 * higher support ratio → higher volume → heavier box → lower z.
	 *
	 * <p>Always resolves to {@link HigherSupportHigherVolumeHigherWeightLowerZComparator}.
	 *
	 * @return a factory with the four stability dimensions pre-added (no constraint dims)
	 */
	public static DefaultPlacementComparatorFactory higherSupportHigherVolumeHigherWeightLowerZ() {
		return emptyFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerZIsBetter();
	}

	// =========================================================================
	// Template builder pattern
	// =========================================================================

	/** Returns a new builder for {@link DefaultPlacementComparatorFactory} with all constraints enabled. */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Fluent builder that configures which constraint dimensions are permanently enabled in the
	 * template and which position tiebreakers are appended.
	 *
	 * <p>Use to create a shared template once; call {@link DefaultPlacementComparatorFactory#create}
	 * or {@link DefaultPlacementComparatorFactory#withConstraints} per packing run.
	 *
	 * <p>By default the builder uses the {@link #defaultRegistry() default registry}. To extend
	 * it with custom fused comparators use {@link #addRegistryEntry}; to replace it entirely
	 * use {@link #withRegistry}.
	 */
	public static final class Builder {

		private boolean weightEnabled    = true;
		private boolean pressureEnabled  = true;
		private boolean countEnabled     = true;
		private boolean identicalEnabled = true;
		private Consumer<DefaultPlacementComparatorFactory> positionConfigurer;
		/** {@code null} means "use DEFAULT_REGISTRY"; non-null is a custom/extended list. */
		private List<RegistryEntry> registry;

		private Builder() {}

		/**
		 * Controls whether the max-load weight dimension is present in the template.
		 * When {@code false} it is permanently removed; per-run weight flags are ignored.
		 * Default: {@code true}.
		 */
		public Builder withWeightEnabled(boolean enabled) {
			this.weightEnabled = enabled;
			return this;
		}

		/** Controls whether the max-load pressure dimension is present in the template. Default: {@code true}. */
		public Builder withPressureEnabled(boolean enabled) {
			this.pressureEnabled = enabled;
			return this;
		}

		/** Controls whether the max-load box count dimension is present in the template. Default: {@code true}. */
		public Builder withCountEnabled(boolean enabled) {
			this.countEnabled = enabled;
			return this;
		}

		/** Controls whether the identical-only restriction dimension is present in the template. Default: {@code true}. */
		public Builder withIdenticalEnabled(boolean enabled) {
			this.identicalEnabled = enabled;
			return this;
		}

		/**
		 * Sets a configurer that appends position / size dimensions (e.g. lower-z, higher-volume)
		 * to the template after all constraint dimensions.
		 *
		 * @param configurer consumer that adds position dims to the template; {@code null} to clear
		 */
		public Builder withPositionDimensions(Consumer<DefaultPlacementComparatorFactory> configurer) {
			this.positionConfigurer = configurer;
			return this;
		}

		/**
		 * Replaces the entire optimized registry with the supplied entries.
		 * The entries are sorted longest-first at build time.
		 *
		 * <p>Use {@link DefaultPlacementComparatorFactory#registryEntry} to create entries
		 * without the verbose generic types, and {@link DefaultPlacementComparatorFactory#defaultRegistry()}
		 * to start from the built-in presets.
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
		 * <pre>
		 *  builder.addRegistryEntry(
		 *          List.of(HIGHER_VOLUME, LOWER_Z),
		 *          MyVolumeZComparator::new);
		 * </pre>
		 *
		 * @param attributes ordered list of attributes that form the match prefix
		 * @param factory    constructor reference or lambda that creates the fused comparator
		 * @return {@code this}
		 */
		public Builder addRegistryEntry(
				List<PlacementComparatorAttribute> attributes,
				Function<PlacementComparator, AbstractChainedPlacementComparator> factory) {
			if (registry == null) {
				registry = new ArrayList<>(DEFAULT_REGISTRY);
			}
			registry.add(new RegistryEntry(attributes, factory));
			return this;
		}

		/**
		 * Resets the registry to the built-in {@link #defaultRegistry() default}.
		 * Useful after a {@link #withRegistry} call if you changed your mind.
		 *
		 * @return {@code this}
		 */
		public Builder withDefaultRegistry() {
			this.registry = null;
			return this;
		}

		/**
		 * Builds the {@link DefaultPlacementComparatorFactory} template.
		 *
		 * <p>Starts from {@link DefaultPlacementComparatorFactory#newFactory()} (all 4 constraint types
		 * pre-loaded), removes disabled types via
		 * {@link DefaultPlacementComparatorFactory#withoutAttribute(PlacementComparatorAttribute)},
		 * then applies any position configurer and registry.
		 */
		public DefaultPlacementComparatorFactory build() {
			DefaultPlacementComparatorFactory template = registry != null
					? new DefaultPlacementComparatorFactory(registry)
					: new DefaultPlacementComparatorFactory();
			// Pre-load constraint entries (matching newFactory() ordering)
			template.entries.add(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
					NoIdenticalConstraintComparator::new));
			template.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
					HigherMaxLoadBoxCountComparator::new));
			template.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
					HigherMaxLoadWeightComparator::new));
			template.entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
					HigherMaxLoadPressureComparator::new));
			if (!weightEnabled)    template.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT);
			if (!pressureEnabled)  template.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE);
			if (!countEnabled)     template.withoutAttribute(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT);
			if (!identicalEnabled) template.withoutAttribute(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT);
			if (positionConfigurer != null) positionConfigurer.accept(template);
			return template;
		}
	}

	// =========================================================================
	// Clone with constraint parameters
	// =========================================================================

	/**
	 * Returns a new factory containing only the constraint entries whose type is active,
	 * plus all position / size entries (which are never filtered), in their original order.
	 * The clone inherits this factory's {@link #optimizedRegistry}.
	 *
	 * @param weight    include weight-constraint entry if present
	 * @param pressure  include pressure-constraint entry if present
	 * @param count     include count-constraint entry if present
	 * @param identical include identical-only-constraint entry if present
	 * @return a new, independently mutable {@link DefaultPlacementComparatorFactory}
	 */
	public DefaultPlacementComparatorFactory withConstraints(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = buildAvailableSet(weight, pressure, count, identical);
		DefaultPlacementComparatorFactory clone = new DefaultPlacementComparatorFactory(optimizedRegistry);
		for (AttrEntry e : entries) {
			if (!e.attribute().isSkippable(available)) {
				clone.entries.add(e);
			}
		}
		return clone;
	}

	/** Convenience alias for {@link #withConstraints(boolean, boolean, boolean, boolean)}. */
	public DefaultPlacementComparatorFactory createBuilder(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		return withConstraints(weight, pressure, count, identical);
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
	 * Returns a new factory with the same entries and registry as this one, but with an
	 * additional registry entry prepended (longer prefixes sort before shorter ones at
	 * build time). The current instance is not modified.
	 *
	 * <p>Useful when you want to extend the default registry with a custom fused comparator
	 * for a specific dimension sequence:
	 *
	 * <pre>
	 *  DefaultPlacementComparatorFactory f = DefaultPlacementComparatorFactory.newFactory()
	 *          .higherVolumeIsBetter().lowerZIsBetter()
	 *          .withAddedRegistryEntry(
	 *                  List.of(HIGHER_VOLUME, LOWER_Z),
	 *                  MyVolumeZComparator::new);
	 * </pre>
	 *
	 * @param attributes ordered list of attributes that form the match prefix
	 * @param factory    constructor reference or lambda that creates the fused comparator
	 * @return a new factory with the extended registry
	 */
	public DefaultPlacementComparatorFactory withAddedRegistryEntry(
			List<PlacementComparatorAttribute> attributes,
			Function<PlacementComparator, AbstractChainedPlacementComparator> factory) {
		List<RegistryEntry> extended = new ArrayList<>(optimizedRegistry);
		extended.add(new RegistryEntry(attributes, factory));
		DefaultPlacementComparatorFactory clone = new DefaultPlacementComparatorFactory(extended);
		clone.entries.addAll(this.entries);
		return clone;
	}

	private static Set<String> buildAvailableSet(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = new HashSet<>(4);
		if (weight)   available.add("weight");
		if (pressure) available.add("pressure");
		if (count)    available.add("count");
		if (identical) available.add("identical");
		return available;
	}

	/**
	 * Removes all entries whose {@link PlacementComparatorAttribute#getId() attribute ID}
	 * matches that of the given attribute, in-place. Returns {@code this}.
	 *
	 * @param attribute the attribute whose entries to remove; must not be {@code null}
	 * @return {@code this}
	 */
	public DefaultPlacementComparatorFactory withoutAttribute(PlacementComparatorAttribute attribute) {
		entries.removeIf(e -> e.attribute().getId().equals(attribute.getId()));
		return this;
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

	// =========================================================================
	// X coordinate
	// =========================================================================

	/** Prefer placements with a <b>lower x</b> coordinate. */
	public DefaultPlacementComparatorFactory lowerXIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_X, LowerXComparator::new));
		return this;
	}

	/** Prefer placements with a <b>higher x</b> coordinate. */
	public DefaultPlacementComparatorFactory higherXIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_X, HigherXComparator::new));
		return this;
	}

	// =========================================================================
	// Y coordinate
	// =========================================================================

	/** Prefer placements with a <b>lower y</b> coordinate. */
	public DefaultPlacementComparatorFactory lowerYIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_Y, LowerYComparator::new));
		return this;
	}

	/** Prefer placements with a <b>higher y</b> coordinate. */
	public DefaultPlacementComparatorFactory higherYIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_Y, HigherYComparator::new));
		return this;
	}

	// =========================================================================
	// Z coordinate
	// =========================================================================

	/** Prefer placements with a <b>lower z</b> coordinate (floor-first strategy). */
	public DefaultPlacementComparatorFactory lowerZIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_Z, LowerZComparator::new));
		return this;
	}

	/** Prefer placements with a <b>higher z</b> coordinate. */
	public DefaultPlacementComparatorFactory higherZIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_Z, HigherZComparator::new));
		return this;
	}

	// =========================================================================
	// Footprint area  (dx × dy)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger footprint area</b>. */
	public DefaultPlacementComparatorFactory higherAreaIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_AREA, HigherAreaComparator::new));
		return this;
	}

	/** Prefer placements where the box has a <b>smaller footprint area</b>. */
	public DefaultPlacementComparatorFactory lowerAreaIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_AREA, LowerAreaComparator::new));
		return this;
	}

	// =========================================================================
	// Volume  (dx × dy × dz)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger volume</b>. */
	public DefaultPlacementComparatorFactory higherVolumeIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_VOLUME, HigherVolumeComparator::new));
		return this;
	}

	/** Prefer placements where the box has a <b>smaller volume</b>. */
	public DefaultPlacementComparatorFactory lowerVolumeIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_VOLUME, LowerVolumeComparator::new));
		return this;
	}

	// =========================================================================
	// Box weight (physical weight of the box)
	// =========================================================================

	/** Prefer placements where the box is <b>heavier</b>. */
	public DefaultPlacementComparatorFactory higherWeightIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_WEIGHT, HigherWeightComparator::new));
		return this;
	}

	/** Prefer placements where the box is <b>lighter</b>. */
	public DefaultPlacementComparatorFactory lowerWeightIsBetter() {
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
	public DefaultPlacementComparatorFactory higherSupportIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_SUPPORT, HigherSupportComparator::new));
		return this;
	}

	/** Prefer placements with a <b>lower support ratio</b>. */
	public DefaultPlacementComparatorFactory lowerSupportIsBetter() {
		entries.add(new AttrEntry(PlacementComparatorAttribute.LOWER_SUPPORT, LowerSupportComparator::new));
		return this;
	}

	// =========================================================================
	// Max-load weight limit  (replaceOrAdd semantics — keeps constraint priority order)
	// =========================================================================

	/** Prefer placements where the box allows a <b>higher max-load weight</b> on top. Unconstrained = unlimited. */
	public DefaultPlacementComparatorFactory higherMaxLoadWeightIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
				HigherMaxLoadWeightComparator::new));
		return this;
	}

	/** Prefer placements where the box allows a <b>lower max-load weight</b> on top. */
	public DefaultPlacementComparatorFactory lowerMaxLoadWeightIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_WEIGHT,
				LowerMaxLoadWeightComparator::new));
		return this;
	}

	// =========================================================================
	// Max-load pressure limit
	// =========================================================================

	/** Prefer placements where the box tolerates a <b>higher max-load pressure</b> on top. */
	public DefaultPlacementComparatorFactory higherMaxLoadPressureIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
				HigherMaxLoadPressureComparator::new));
		return this;
	}

	/** Prefer placements where the box tolerates a <b>lower max-load pressure</b> on top. */
	public DefaultPlacementComparatorFactory lowerMaxLoadPressureIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_PRESSURE,
				LowerMaxLoadPressureComparator::new));
		return this;
	}

	// =========================================================================
	// Max-load box count limit
	// =========================================================================

	/** Prefer placements where the box allows a <b>higher number of boxes</b> stacked on top. */
	public DefaultPlacementComparatorFactory higherMaxLoadBoxCountIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
				HigherMaxLoadBoxCountComparator::new));
		return this;
	}

	/** Prefer placements where the box allows a <b>lower number of boxes</b> on top. */
	public DefaultPlacementComparatorFactory lowerMaxLoadBoxCountIsBetter() {
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
	public DefaultPlacementComparatorFactory noIdenticalConstraintIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
				NoIdenticalConstraintComparator::new));
		return this;
	}

	/** Prefer placements where the box <b>has</b> the identical-only restriction. */
	public DefaultPlacementComparatorFactory identicalConstraintIsBetter() {
		replaceOrAdd(new AttrEntry(PlacementComparatorAttribute.IDENTICAL_CONSTRAINT,
				IdenticalConstraintComparator::new));
		return this;
	}

	// =========================================================================
	// Build — implements PlacementComparatorFactory
	// =========================================================================

	/**
	 * Builds and returns the configured comparator, skipping any entry whose attribute appears
	 * in {@code disabled}.
	 *
	 * <p>Scans the per-instance {@link #optimizedRegistry} for the longest matching attribute
	 * prefix in the active entry list. When a match is found the corresponding fused comparator
	 * is used; the remaining suffix is chained as its {@code next}. When no prefix matches a
	 * plain per-dimension chain is produced.
	 *
	 * @param disabled attributes to skip; must not be {@code null}
	 * @return a non-null {@link PlacementComparator}; the no-op comparator if no active entries
	 */
	@Override
	public PlacementComparator build(Collection<PlacementComparatorAttribute> disabled) {
		List<AttrEntry> active = disabled.isEmpty() ? entries : filtered(disabled);
		return buildFrom(active);
	}

	private List<AttrEntry> filtered(Collection<PlacementComparatorAttribute> disabled) {
		// Match by attribute ID so any representative constant for a constraint type
		// (e.g. HIGHER_MAX_LOAD_WEIGHT) disables all entries with that ID ("weight"),
		// regardless of whether the factory was configured with the higher or lower variant.
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
		List<Function<PlacementComparator, AbstractChainedPlacementComparator>> factories
				= new ArrayList<>(active.size());
		for (AttrEntry e : active) {
			attrs.add(e.attribute());
			factories.add(e.factory());
		}

		for (RegistryEntry entry : optimizedRegistry) {
			List<PlacementComparatorAttribute> combo = entry.attributes();
			if (attrs.size() >= combo.size() && attrs.subList(0, combo.size()).equals(combo)) {
				PlacementComparator suffix = buildChain(factories, combo.size(), attrs.size());
				return entry.factory().apply(suffix);
			}
		}

		return buildChain(factories, 0, attrs.size());
	}

	private static PlacementComparator buildChain(
			List<Function<PlacementComparator, AbstractChainedPlacementComparator>> factories,
			int start, int end) {
		PlacementComparator result = null;
		for (int i = end - 1; i >= start; i--) {
			result = factories.get(i).apply(result);
		}
		return result;
	}

	// =========================================================================
	// replaceOrAdd helper
	// =========================================================================

	/**
	 * Replaces the first entry whose attribute ID matches that of {@code entry} in-place,
	 * preserving the priority order. If no such entry exists, appends to the end.
	 */
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

	/** Prefers higher support ratio ({@code supportedArea / (dx × dy)}). */
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

	/** Prefers placements where the box has <b>no</b> identical-only restriction. */
	public static final class NoIdenticalConstraintComparator extends AbstractChainedPlacementComparator {
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
	 *  │  A: box 6×6, supportedArea=36 (100%)  ┌──────┐ fully supported  │
	 *  │  B: box 6×6, supportedArea=18 (50%)   ┌──────┐ overhanging      │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — volume (higher is better)                         │
	 *  │  A: 4×4×4=64  ┌────┐  B: 2×2×2=8  ┌──┐  → A preferred         │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — box weight (higher is better)                     │
	 *  │  A: weight=50  ████████  B: weight=5  ░░  → A preferred         │
	 *  ├──────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — z coordinate (lower is better, floor-first)       │
	 *  │  z=0 ┌──┐ A preferred     z=5 ┌──┐ B  ════ floor               │
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
	 *  │  A: accepts any box  ■ ● ▲    B: identical only  ■ ■ ■              │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — max box count on top (higher is better)               │
	 *  │  A: limit=10  ┌──┬──┬──┐…    B: limit=2  ┌──┬──┐  → A preferred   │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — max load weight (higher is better; ∞ = unconstrained) │
	 *  │  A: 500 kg  ░░░░░░░░░░░    B: 100 kg  ░░░  → A preferred           │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — max load pressure (higher is better)                  │
	 *  │  A: 8.0 N/cm²  ▓▓▓▓▓    B: 2.0 N/cm²  ▓▓  → A preferred          │
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
