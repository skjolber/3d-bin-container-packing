package com.github.skjolber.packing.comparator.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	private static final class AttrEntry {
		private final PlacementComparatorAttribute attribute;
		private final PlacementComparatorSupplier supplier;

		AttrEntry(PlacementComparatorAttribute attribute, PlacementComparatorSupplier supplier) {
			this.attribute = attribute;
			this.supplier = supplier;
		}

		PlacementComparatorAttribute attribute() { return attribute; }
		PlacementComparatorSupplier supplier() { return supplier; }

		@Override
		public String toString() {
			return attribute.getId() + "(" + (attribute.higherIsBetter() ? "↑" : "↓") + ")";
		}
	}

	// =========================================================================
	// Default optimized registry
	// =========================================================================

	/** Built-in registry list — kept for the public {@link #defaultRegistry()} API. */
	private static final List<RegistryEntry> DEFAULT_REGISTRY;
	/** Built-in registry as a map for O(1) prefix lookup during {@link #buildFrom}. */
	private static final Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> DEFAULT_REGISTRY_MAP;

	static {
		List<RegistryEntry> reg = new ArrayList<>();

		// --- 9-attr full chains (position + support + constraints) ---
		// higher support → higher volume → heavier box → smaller area → lower z → constraints
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT, PlacementComparatorAttribute.LOWER_AREA,
						PlacementComparatorAttribute.LOWER_Z,
						PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				ConstraintsHigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator::new));

		// higher support → lower z → larger area → higher volume → heavier box → constraints
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.LOWER_Z,
						PlacementComparatorAttribute.HIGHER_AREA, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT,
						PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				ConstraintsHigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator::new));

		// --- 8-attr full chains (position + constraints, no support) ---
		// higher volume → heavier box → smaller area → lower z → constraints  (PlainPackager / LAFF placement)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_VOLUME, PlacementComparatorAttribute.HIGHER_WEIGHT,
						PlacementComparatorAttribute.LOWER_AREA, PlacementComparatorAttribute.LOWER_Z,
						PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				ConstraintsHigherVolumeHigherWeightLowerAreaLowerZComparator::new));

		// lower z → larger area → higher volume → heavier box → constraints  (LAFF / FastLAFF first-placement)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.LOWER_Z, PlacementComparatorAttribute.HIGHER_AREA,
						PlacementComparatorAttribute.HIGHER_VOLUME, PlacementComparatorAttribute.HIGHER_WEIGHT,
						PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				ConstraintsLowerZHigherAreaHigherVolumeHigherWeightComparator::new));

		// --- 5-attr position chains (with support) ---
		// higher support → higher volume → heavier box → smaller area → lower z
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT, PlacementComparatorAttribute.LOWER_AREA,
						PlacementComparatorAttribute.LOWER_Z),
				HigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator::new));

		// higher support → lower z → larger area → higher volume → heavier box
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.LOWER_Z,
						PlacementComparatorAttribute.HIGHER_AREA, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT),
				HigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator::new));

		// --- 4-attr position chains ---
		// higher support → higher volume → heavier box → lower z  (placement stability preset)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_SUPPORT, PlacementComparatorAttribute.HIGHER_VOLUME,
						PlacementComparatorAttribute.HIGHER_WEIGHT, PlacementComparatorAttribute.LOWER_Z),
				HigherSupportHigherVolumeHigherWeightLowerZComparator::new));

		// higher volume → heavier box → smaller area → lower z
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.HIGHER_VOLUME, PlacementComparatorAttribute.HIGHER_WEIGHT,
						PlacementComparatorAttribute.LOWER_AREA, PlacementComparatorAttribute.LOWER_Z),
				HigherVolumeHigherWeightLowerAreaLowerZComparator::new));

		// lower z → larger area → higher volume → heavier box
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.LOWER_Z, PlacementComparatorAttribute.HIGHER_AREA,
						PlacementComparatorAttribute.HIGHER_VOLUME, PlacementComparatorAttribute.HIGHER_WEIGHT),
				LowerZHigherAreaHigherVolumeHigherWeightComparator::new));

		// no-identical → high count → high load weight → high pressure  (constraint-only fused)
		reg.add(new RegistryEntry(
				List.of(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
						PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT, PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE),
				NoIdenticalHigherCountHigherWeightHigherPressureComparator::new));

		DEFAULT_REGISTRY = Collections.unmodifiableList(reg);
		DEFAULT_REGISTRY_MAP = buildRegistryMap(reg);
	}

	private static Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> buildRegistryMap(
			List<RegistryEntry> entries) {
		Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> map =
				new HashMap<>(entries.size() * 2);
		for (RegistryEntry e : entries) map.put(e.attributes(), e.supplier());
		return map;
	}

	// =========================================================================
	// Immutable instance fields (compiled factory)
	// =========================================================================

	/** Attribute for each configured dimension (parallel to {@link #supplierKeys}). */
	private final PlacementComparatorAttribute[] attrKeys;
	/** Supplier for each configured dimension (parallel to {@link #attrKeys}). */
	private final PlacementComparatorSupplier[] supplierKeys;
	/** Registry for O(1) prefix lookup during {@link #buildFrom}. */
	private final Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> registry;

	// =========================================================================
	// Package-private constructors (used by Builder.compile() and clone methods)
	// =========================================================================

	/** Converts a list of entries + pre-built map into a compiled factory. No sorting. */
	DefaultPlacementComparatorFactory(List<AttrEntry> entries,
			Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> registry) {
		int n = entries.size();
		this.attrKeys = new PlacementComparatorAttribute[n];
		this.supplierKeys = new PlacementComparatorSupplier[n];
		for (int i = 0; i < n; i++) {
			AttrEntry e = entries.get(i);
			attrKeys[i] = e.attribute();
			supplierKeys[i] = e.supplier();
		}
		this.registry = registry;
	}

	/** Direct array constructor — arrays are used as-is (caller must not mutate them). */
	private DefaultPlacementComparatorFactory(PlacementComparatorAttribute[] attrKeys,
			PlacementComparatorSupplier[] supplierKeys,
			Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> registry) {
		this.attrKeys = attrKeys;
		this.supplierKeys = supplierKeys;
		this.registry = registry;
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
		b.trailingEntries.add(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
				NoIdenticalConstraintComparator::new));
		b.trailingEntries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
				HigherMaxLoadBoxCountComparator::new));
		b.trailingEntries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
				HigherMaxLoadWeightComparator::new));
		b.trailingEntries.add(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
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
	 * <p>Uses the per-instance registry map for O(1) prefix lookup. Tries the longest prefix
	 * first, falling back to shorter prefixes, then a plain per-dimension chain.
	 *
	 * @param disabled attributes to skip; must not be {@code null}
	 * @return a non-null {@link PlacementComparator}; the no-op comparator if no active entries
	 */
	@Override
	public PlacementComparator build(Collection<PlacementComparatorAttribute> disabled) {
		if (disabled.isEmpty()) return buildFrom(attrKeys, supplierKeys);
		Set<String> disabledIds = new HashSet<>(disabled.size() * 2);
		for (PlacementComparatorAttribute a : disabled) disabledIds.add(a.getId());
		return buildFiltered(disabledIds);
	}

	// =========================================================================
	// Clone with constraint parameters
	// =========================================================================

	/**
	 * Returns a new compiled factory containing only the constraint entries whose type is active,
	 * plus all position / size entries, in their original order.
	 * Returns {@code this} when all entries survive filtering (no allocation).
	 *
	 * @param weight    include weight-constraint entry if present
	 * @param pressure  include pressure-constraint entry if present
	 * @param count     include count-constraint entry if present
	 * @param identical include identical-only-constraint entry if present
	 * @return a (possibly same) {@link DefaultPlacementComparatorFactory}
	 */
	public DefaultPlacementComparatorFactory withConstraints(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = buildAvailableSet(weight, pressure, count, identical);
		int n = 0;
		for (PlacementComparatorAttribute a : attrKeys) if (!a.isSkippable(available)) n++;
		if (n == attrKeys.length) return this;
		PlacementComparatorAttribute[] fa = new PlacementComparatorAttribute[n];
		PlacementComparatorSupplier[] fs = new PlacementComparatorSupplier[n];
		int j = 0;
		for (int i = 0; i < attrKeys.length; i++) {
			if (!attrKeys[i].isSkippable(available)) { fa[j] = attrKeys[i]; fs[j] = supplierKeys[i]; j++; }
		}
		return new DefaultPlacementComparatorFactory(fa, fs, registry);
	}

	/**
	 * Builds a {@link PlacementComparator} for the given constraint profile directly,
	 * without creating an intermediate factory object.
	 *
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	public PlacementComparator create(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		Set<String> available = buildAvailableSet(weight, pressure, count, identical);
		int n = 0;
		for (PlacementComparatorAttribute a : attrKeys) if (!a.isSkippable(available)) n++;
		if (n == attrKeys.length) return buildFrom(attrKeys, supplierKeys);
		PlacementComparatorAttribute[] fa = new PlacementComparatorAttribute[n];
		PlacementComparatorSupplier[] fs = new PlacementComparatorSupplier[n];
		int j = 0;
		for (int i = 0; i < attrKeys.length; i++) {
			if (!attrKeys[i].isSkippable(available)) { fa[j] = attrKeys[i]; fs[j] = supplierKeys[i]; j++; }
		}
		return buildFrom(fa, fs);
	}

	/**
	 * Alias for {@link #create(boolean, boolean, boolean, boolean)}.
	 */
	public PlacementComparator newInstance(boolean weight, boolean pressure,
			boolean count, boolean identical) {
		return create(weight, pressure, count, identical);
	}

	/**
	 * Returns a new factory with the matching attribute entries removed.
	 * Returns {@code this} if the attribute is not present (no allocation).
	 *
	 * @param attribute the attribute whose entries to remove; must not be {@code null}
	 * @return a (possibly same) factory without the matching entries
	 */
	public DefaultPlacementComparatorFactory withoutAttribute(PlacementComparatorAttribute attribute) {
		String id = attribute.getId();
		int n = 0;
		for (PlacementComparatorAttribute a : attrKeys) if (!a.getId().equals(id)) n++;
		if (n == attrKeys.length) return this;
		PlacementComparatorAttribute[] fa = new PlacementComparatorAttribute[n];
		PlacementComparatorSupplier[] fs = new PlacementComparatorSupplier[n];
		int j = 0;
		for (int i = 0; i < attrKeys.length; i++) {
			if (!attrKeys[i].getId().equals(id)) { fa[j] = attrKeys[i]; fs[j] = supplierKeys[i]; j++; }
		}
		return new DefaultPlacementComparatorFactory(fa, fs, registry);
	}

	/**
	 * Returns a new factory with the same entries and registry plus one additional registry entry.
	 * The current instance is not modified.
	 *
	 * @param attributes ordered list of attributes that form the match prefix
	 * @param supplier   constructor reference or lambda that creates the fused comparator
	 * @return a new factory with the extended registry
	 */
	public DefaultPlacementComparatorFactory withAddedRegistryEntry(
			List<PlacementComparatorAttribute> attributes,
			PlacementComparatorSupplier supplier) {
		Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> extended =
				new HashMap<>(registry);
		extended.put(List.copyOf(attributes), supplier);
		return new DefaultPlacementComparatorFactory(attrKeys, supplierKeys, extended);
	}

	// =========================================================================
	// Build internals
	// =========================================================================

	/** Filters by a pre-built set of disabled IDs, then builds. */
	private PlacementComparator buildFiltered(Set<String> disabledIds) {
		int n = 0;
		for (PlacementComparatorAttribute a : attrKeys) if (!disabledIds.contains(a.getId())) n++;
		if (n == attrKeys.length) return buildFrom(attrKeys, supplierKeys);
		PlacementComparatorAttribute[] fa = new PlacementComparatorAttribute[n];
		PlacementComparatorSupplier[] fs = new PlacementComparatorSupplier[n];
		int j = 0;
		for (int i = 0; i < attrKeys.length; i++) {
			if (!disabledIds.contains(attrKeys[i].getId())) { fa[j] = attrKeys[i]; fs[j] = supplierKeys[i]; j++; }
		}
		return buildFrom(fa, fs);
	}

	/**
	 * Builds a comparator chain from the given parallel attribute/supplier arrays.
	 *
	 * <p>Registry lookup: tries the full attribute list as a map key, then progressively
	 * shorter prefixes (longest-first). Each lookup is O(1) average. When a fused comparator
	 * is found for a prefix, the remaining suffix is chained behind it. Falls back to a plain
	 * per-dimension chain when no prefix matches.
	 */
	private PlacementComparator buildFrom(PlacementComparatorAttribute[] attrs,
			PlacementComparatorSupplier[] suppliers) {
		int len = attrs.length;
		if (len == 0) return PlacementComparator.noOp();
		List<PlacementComparatorAttribute> full = Arrays.asList(attrs);
		for (int matchLen = len; matchLen >= 1; matchLen--) {
			PlacementComparatorSupplier s = registry.get(
					matchLen == len ? full : full.subList(0, matchLen));
			if (s != null) {
				PlacementComparator suffix = buildChain(suppliers, matchLen, len);
				return link(s.get(), suffix);
			}
		}
		return buildChain(suppliers, 0, len);
	}

	private static PlacementComparator buildChain(
			PlacementComparatorSupplier[] suppliers, int start, int end) {
		PlacementComparator result = null;
		for (int i = end - 1; i >= start; i--) {
			result = link(suppliers[i].get(), result);
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
		if (weight)    available.add(PlacementComparatorAttribute.ID_WEIGHT);
		if (pressure)  available.add(PlacementComparatorAttribute.ID_PRESSURE);
		if (count)     available.add(PlacementComparatorAttribute.ID_COUNT);
		if (identical) available.add(PlacementComparatorAttribute.ID_IDENTICAL);
		return available;
	}

	// =========================================================================
	// Builder inner class
	// =========================================================================

	/**
	 * Mutable builder for {@link DefaultPlacementComparatorFactory}.
	 *
	 * <p>Position and constraint entries may be added in any order via their respective fluent
	 * methods; the resulting chain reflects that call order exactly.  The one exception is that
	 * constraints pre-loaded by {@link DefaultPlacementComparatorFactory#newFactory()} are placed
	 * in a separate <em>trailing</em> list that is always appended <em>after</em> any user-supplied
	 * entries at {@link #compile()} time.  If the caller explicitly invokes a constraint method
	 * (e.g. {@link #higherMaxLoadWeightIsBetter()}) those defaults are moved out of the trailing
	 * list and inserted at the caller's chosen position.
	 *
	 * <p>Returned by {@link DefaultPlacementComparatorFactory#newFactory()},
	 * {@link DefaultPlacementComparatorFactory#newBuilder()}, and
	 * {@link DefaultPlacementComparatorFactory#emptyFactory()}.
	 *
	 * <p>Call {@link #compile()} to obtain an immutable {@link DefaultPlacementComparatorFactory}
	 * that can be reused as a template.  Call {@link #build()} to get a
	 * {@link PlacementComparator} directly.
	 */
	public static final class Builder implements PlacementComparatorFactory {

		/** User-specified entries in call order — may contain both positions and constraints. */
		private final List<AttrEntry> entries = new ArrayList<>();
		/**
		 * Constraint entries pre-loaded by {@link DefaultPlacementComparatorFactory#newFactory()}.
		 * Always appended <em>after</em> {@link #entries} at compile time.
		 * Entries are removed here when the user explicitly calls the matching constraint method.
		 */
		private final List<AttrEntry> trailingEntries = new ArrayList<>();
		/** {@code null} means "use DEFAULT_REGISTRY"; non-null is a custom/extended list. */
		private List<RegistryEntry> registry;
		/** Applied to a temp builder at {@link #compile()} time; entries are inserted before trailing. */
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
		// Max-load weight limit
		// =========================================================================

		/**
		 * Prefer placements where the box allows a <b>higher max-load weight</b> on top.
		 * If a trailing default entry for this attribute exists it is replaced in-place here.
		 */
		public Builder higherMaxLoadWeightIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT,
					HigherMaxLoadWeightComparator::new));
			return this;
		}

		/** Prefer placements where the box allows a <b>lower max-load weight</b> on top. */
		public Builder lowerMaxLoadWeightIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_WEIGHT,
					LowerMaxLoadWeightComparator::new));
			return this;
		}

		// =========================================================================
		// Max-load pressure limit
		// =========================================================================

		/** Prefer placements where the box tolerates a <b>higher max-load pressure</b> on top. */
		public Builder higherMaxLoadPressureIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE,
					HigherMaxLoadPressureComparator::new));
			return this;
		}

		/** Prefer placements where the box tolerates a <b>lower max-load pressure</b> on top. */
		public Builder lowerMaxLoadPressureIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_PRESSURE,
					LowerMaxLoadPressureComparator::new));
			return this;
		}

		// =========================================================================
		// Max-load box count limit
		// =========================================================================

		/** Prefer placements where the box allows a <b>higher number of boxes</b> stacked on top. */
		public Builder higherMaxLoadBoxCountIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT,
					HigherMaxLoadBoxCountComparator::new));
			return this;
		}

		/** Prefer placements where the box allows a <b>lower number of boxes</b> on top. */
		public Builder lowerMaxLoadBoxCountIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.LOWER_MAX_LOAD_BOX_COUNT,
					LowerMaxLoadBoxCountComparator::new));
			return this;
		}

		// =========================================================================
		// Identical-only restriction
		// =========================================================================

		/**
		 * Prefer placements where the box has <b>no identical-only restriction</b>.
		 * If a trailing default entry for this attribute exists it is replaced in-place here.
		 */
		public Builder noIdenticalConstraintIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT,
					NoIdenticalConstraintComparator::new));
			return this;
		}

		/** Prefer placements where the box <b>has</b> the identical-only restriction. */
		public Builder identicalConstraintIsBetter() {
			promoteOrAdd(new AttrEntry(PlacementComparatorAttribute.IDENTICAL_CONSTRAINT,
					IdenticalConstraintComparator::new));
			return this;
		}

		// =========================================================================
		// Clone with constraint parameters
		// =========================================================================

		/**
		 * Returns a new builder that keeps all user-specified entries and only those trailing
		 * constraint entries whose type is currently active.
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
			clone.positionConfigurer = this.positionConfigurer;
			for (AttrEntry e : entries) {
				if (!e.attribute().isSkippable(available)) {
					clone.entries.add(e);
				}
			}
			for (AttrEntry e : trailingEntries) {
				if (!e.attribute().isSkippable(available)) {
					clone.trailingEntries.add(e);
				}
			}
			return clone;
		}

		/**
		 * Removes all entries whose attribute ID matches that of the given attribute, in-place.
		 * Applies to both {@code entries} and {@code trailingEntries}.
		 *
		 * @param attribute the attribute whose entries to remove; must not be {@code null}
		 * @return {@code this}
		 */
		public Builder withoutAttribute(PlacementComparatorAttribute attribute) {
			String id = attribute.getId();
			entries.removeIf(e -> e.attribute().getId().equals(id));
			trailingEntries.removeIf(e -> e.attribute().getId().equals(id));
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
		 * Sets a configurer that adds additional entries (typically position dimensions)
		 * to the template.  Applied to a temporary builder at {@link #compile()} time;
		 * resulting entries are inserted between {@link #entries} and {@link #trailingEntries}.
		 *
		 * @param configurer consumer that adds dims to a temp builder; {@code null} to clear
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
		 * Builds an immutable {@link DefaultPlacementComparatorFactory}.
		 *
		 * <p>Compiled entry order:
		 * <ol>
		 *   <li>{@link #entries} — user-specified entries in call order</li>
		 *   <li>configurer entries (from {@link #withPositionDimensions}, if set)</li>
		 *   <li>{@link #trailingEntries} — pre-loaded defaults from {@link DefaultPlacementComparatorFactory#newFactory()}</li>
		 * </ol>
		 *
		 * @return a compiled, immutable factory
		 */
		public DefaultPlacementComparatorFactory compile() {
			List<AttrEntry> toCompile = new ArrayList<>(entries);
			if (positionConfigurer != null) {
				Builder temp = new Builder();
				positionConfigurer.accept(temp);
				toCompile.addAll(temp.entries);
				toCompile.addAll(temp.trailingEntries);
			}
			toCompile.addAll(trailingEntries);
			Map<List<PlacementComparatorAttribute>, PlacementComparatorSupplier> regMap =
					registry == null ? DEFAULT_REGISTRY_MAP : buildRegistryMap(registry);
			return new DefaultPlacementComparatorFactory(toCompile, regMap);
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

		/**
		 * Removes any matching entry from {@code trailingEntries} (pre-loaded defaults) and
		 * appends (or replaces within {@code entries}) the given entry.
		 * This ensures user-called constraint methods take the position in the chain where they
		 * are called, rather than remaining at the end as trailing defaults.
		 */
		private void promoteOrAdd(AttrEntry entry) {
			String id = entry.attribute().getId();
			trailingEntries.removeIf(e -> e.attribute().getId().equals(id));
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

	// =========================================================================
	// Fused comparators — position-only chains
	// =========================================================================

	/**
	 * Fused comparator: higher volume → heavier box → smaller area → lower z.
	 *
	 * <p>Registered for {@code [HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_AREA, LOWER_Z]}.
	 * This is the default <em>position suffix</em> used by PlainPackager and
	 * LargestAreaFitFirstPackager (placement) when no support is calculated.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────┐
	 *  │  Priority 1 — volume (higher is better)             │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 2 — box weight (higher is better)         │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 3 — footprint area (smaller is better)    │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 4 — z coordinate (lower is better)        │
	 *  └──────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class HigherVolumeHigherWeightLowerAreaLowerZComparator
			extends AbstractChainedPlacementComparator {

		public HigherVolumeHigherWeightLowerAreaLowerZComparator() {}

		public HigherVolumeHigherWeightLowerAreaLowerZComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			r = Long.compare(b.getStackValue().getArea(), a.getStackValue().getArea());
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Fused comparator: lower z → larger area → higher volume → heavier box.
	 *
	 * <p>Registered for {@code [LOWER_Z, HIGHER_AREA, HIGHER_VOLUME, HIGHER_WEIGHT]}.
	 * This is the default <em>first-placement</em> position suffix used by
	 * LargestAreaFitFirstPackager and FastLargestAreaFitFirstPackager (no support).
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────┐
	 *  │  Priority 1 — z coordinate (lower is better)        │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 2 — footprint area (larger is better)     │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 3 — volume (higher is better)             │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 4 — box weight (higher is better)         │
	 *  └──────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class LowerZHigherAreaHigherVolumeHigherWeightComparator
			extends AbstractChainedPlacementComparator {

		public LowerZHigherAreaHigherVolumeHigherWeightComparator() {}

		public LowerZHigherAreaHigherVolumeHigherWeightComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Fused comparator: higher support → higher volume → heavier box → smaller area → lower z.
	 *
	 * <p>Registered for {@code [HIGHER_SUPPORT, HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_AREA, LOWER_Z]}.
	 * Position suffix used when support calculation is enabled (PlainPackager / LAFF placement).
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)      │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 2 — volume (higher is better)             │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 3 — box weight (higher is better)         │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 4 — footprint area (smaller is better)    │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 5 — z coordinate (lower is better)        │
	 *  └──────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class HigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator
			extends AbstractChainedPlacementComparator {

		public HigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator() {}

		public HigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator(PlacementComparator next) {
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
			r = Long.compare(b.getStackValue().getArea(), a.getStackValue().getArea());
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			return r != 0 ? r : chain(a, b);
		}
	}

	/**
	 * Fused comparator: higher support → lower z → larger area → higher volume → heavier box.
	 *
	 * <p>Registered for {@code [HIGHER_SUPPORT, LOWER_Z, HIGHER_AREA, HIGHER_VOLUME, HIGHER_WEIGHT]}.
	 * First-placement position suffix when support calculation is enabled (LAFF / FastLAFF).
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)      │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 2 — z coordinate (lower is better)        │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 3 — footprint area (larger is better)     │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 4 — volume (higher is better)             │
	 *  ├──────────────────────────────────────────────────────┤
	 *  │  Priority 5 — box weight (higher is better)         │
	 *  └──────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class HigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator
			extends AbstractChainedPlacementComparator {

		public HigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator() {}

		public HigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(a, b);
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			return r != 0 ? r : chain(a, b);
		}
	}

	// =========================================================================
	// Fused comparators — full constraint + position chains
	// =========================================================================

	/**
	 * Fused comparator for the standard PlainPackager / LAFF placement chain
	 * (no support): higher volume → heavier box → smaller area → lower z → all four constraints.
	 *
	 * <p>Registered for {@code [HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_AREA, LOWER_Z,
	 * NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — volume (higher is better)                             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — box weight (higher is better)                         │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — footprint area (smaller is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — z coordinate (lower is better)                        │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 5 — identical restriction (absence preferred)             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 6 — max box count (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 7 — max load weight (higher is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 8 — max load pressure (higher is better)                  │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class ConstraintsHigherVolumeHigherWeightLowerAreaLowerZComparator
			extends AbstractChainedPlacementComparator {

		public ConstraintsHigherVolumeHigherWeightLowerAreaLowerZComparator() {}

		public ConstraintsHigherVolumeHigherWeightLowerAreaLowerZComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			r = Long.compare(b.getStackValue().getArea(), a.getStackValue().getArea());
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			r = Integer.compare(c1, c2);
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

	/**
	 * Fused comparator for the LAFF / FastLAFF first-placement chain (no support):
	 * lower z → larger area → higher volume → heavier box → all four constraints.
	 *
	 * <p>Registered for {@code [LOWER_Z, HIGHER_AREA, HIGHER_VOLUME, HIGHER_WEIGHT,
	 * NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — z coordinate (lower is better)                        │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — footprint area (larger is better)                     │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — volume (higher is better)                             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — box weight (higher is better)                         │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 5 — identical restriction (absence preferred)             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 6 — max box count (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 7 — max load weight (higher is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 8 — max load pressure (higher is better)                  │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class ConstraintsLowerZHigherAreaHigherVolumeHigherWeightComparator
			extends AbstractChainedPlacementComparator {

		public ConstraintsLowerZHigherAreaHigherVolumeHigherWeightComparator() {}

		public ConstraintsLowerZHigherAreaHigherVolumeHigherWeightComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			r = Integer.compare(c1, c2);
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

	/**
	 * Fused comparator for the PlainPackager / LAFF placement chain with support:
	 * higher support → higher volume → heavier box → smaller area → lower z → all four constraints.
	 *
	 * <p>Registered for {@code [HIGHER_SUPPORT, HIGHER_VOLUME, HIGHER_WEIGHT, LOWER_AREA, LOWER_Z,
	 * NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — volume (higher is better)                             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — box weight (higher is better)                         │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — footprint area (smaller is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 5 — z coordinate (lower is better)                        │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 6 — identical restriction (absence preferred)             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 7 — max box count (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 8 — max load weight (higher is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 9 — max load pressure (higher is better)                  │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class ConstraintsHigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator
			extends AbstractChainedPlacementComparator {

		public ConstraintsHigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator() {}

		public ConstraintsHigherSupportHigherVolumeHigherWeightLowerAreaLowerZComparator(PlacementComparator next) {
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
			r = Long.compare(b.getStackValue().getArea(), a.getStackValue().getArea());
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			r = Integer.compare(c1, c2);
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

	/**
	 * Fused comparator for the LAFF / FastLAFF first-placement chain with support:
	 * higher support → lower z → larger area → higher volume → heavier box → all four constraints.
	 *
	 * <p>Registered for {@code [HIGHER_SUPPORT, LOWER_Z, HIGHER_AREA, HIGHER_VOLUME, HIGHER_WEIGHT,
	 * NO_IDENTICAL_CONSTRAINT, HIGHER_MAX_LOAD_BOX_COUNT, HIGHER_MAX_LOAD_WEIGHT,
	 * HIGHER_MAX_LOAD_PRESSURE]}.
	 *
	 * <pre>
	 *  ┌──────────────────────────────────────────────────────────────────────┐
	 *  │  Priority 1 — support ratio (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 2 — z coordinate (lower is better)                        │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 3 — footprint area (larger is better)                     │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 4 — volume (higher is better)                             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 5 — box weight (higher is better)                         │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 6 — identical restriction (absence preferred)             │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 7 — max box count (higher is better)                      │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 8 — max load weight (higher is better)                    │
	 *  ├──────────────────────────────────────────────────────────────────────┤
	 *  │  Priority 9 — max load pressure (higher is better)                  │
	 *  └──────────────────────────────────────────────────────────────────────┘
	 * </pre>
	 */
	public static final class ConstraintsHigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator
			extends AbstractChainedPlacementComparator {

		public ConstraintsHigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator() {}

		public ConstraintsHigherSupportLowerZHigherAreaHigherVolumeHigherWeightComparator(PlacementComparator next) {
			super(next);
		}

		@Override
		public int compare(Placement a, Placement b) {
			int r = compareSupportRatio(a, b);
			if (r != 0) return r;
			r = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
			if (r != 0) return r;
			r = Long.compare(a.getStackValue().getVolume(), b.getStackValue().getVolume());
			if (r != 0) return r;
			r = Integer.compare(a.getWeight(), b.getWeight());
			if (r != 0) return r;
			boolean aHas = a.getStackValue().isLoadIdenticalBoxOnly();
			boolean bHas = b.getStackValue().isLoadIdenticalBoxOnly();
			if (aHas != bHas) return aHas ? -1 : 1;
			int c1 = a.getStackValue().isMaxLoadBoxCount()
					? a.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			int c2 = b.getStackValue().isMaxLoadBoxCount()
					? b.getStackValue().getMaxLoadBoxCount() : Integer.MAX_VALUE;
			r = Integer.compare(c1, c2);
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
