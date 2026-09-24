package com.github.skjolber.packing.comparator.placement;

import java.util.Collection;
import java.util.List;

/**
 * Factory that builds a {@link PlacementComparator} from a configured set of comparison
 * dimensions, optionally skipping a caller-supplied set of disabled attributes.
 *
 * <p>The default {@link #build()} method builds with no disabled attributes. Callers that need
 * to suppress specific dimensions at build time (e.g. because a constraint is inactive for this
 * packing run) should use {@link #build(Collection)}.
 *
 * <p>The canonical implementation is {@link DefaultPlacementComparatorFactory}, which provides
 * fluent builder methods for adding dimensions, constraint-based cloning via
 * {@code withConstraints}, and an optimized-comparator registry.
 */
public interface PlacementComparatorFactory {

	/**
	 * Builds and returns a {@link PlacementComparator} that skips any entry whose
	 * {@link PlacementComparatorAttribute} appears in {@code disabled}.
	 *
	 * @param disabled attributes whose comparison dimensions should be excluded; must not be
	 *                 {@code null} (pass {@link List#of()} for no exclusions)
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	PlacementComparator build(Collection<PlacementComparatorAttribute> disabled);

	/**
	 * Builds with no disabled attributes; equivalent to {@code build(List.of())}.
	 *
	 * @return a configured, non-null {@link PlacementComparator}
	 */
	default PlacementComparator build() {
		return build(List.of());
	}

	/**
	 * Returns a {@link PlacementComparatorFactory} that always returns the given
	 * fixed comparator, ignoring any disabled attributes.
	 *
	 * <p>Use this bridge when you have a custom {@link PlacementComparator} that
	 * cannot be expressed as a {@link DefaultPlacementComparatorFactory} chain.
	 *
	 * @param comparator the fixed comparator to wrap; must not be {@code null}
	 * @return a factory that always returns {@code comparator}
	 */
	static PlacementComparatorFactory of(PlacementComparator comparator) {
		return disabled -> comparator;
	}
}
