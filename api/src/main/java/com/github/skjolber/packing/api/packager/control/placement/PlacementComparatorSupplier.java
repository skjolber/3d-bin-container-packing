package com.github.skjolber.packing.api.packager.control.placement;

/**
 * Functional interface that supplies a {@link PlacementComparator} instance.
 *
 * <p>Used as the factory type in {@code com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory} registry entries
 * and dimension entries. Unlike {@code Function<PlacementComparator, AbstractChainedPlacementComparator>},
 * this supplier does not accept a {@code next} argument — chaining is handled separately via
 * instanceof check in the build process.
 */
@FunctionalInterface
public interface PlacementComparatorSupplier {

	/**
	 * Creates and returns a new {@link PlacementComparator} instance.
	 *
	 * @return a new comparator; never {@code null}
	 */
	PlacementComparator get();
}
