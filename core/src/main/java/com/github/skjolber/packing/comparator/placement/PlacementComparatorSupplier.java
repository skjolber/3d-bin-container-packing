package com.github.skjolber.packing.comparator.placement;

/**
 * Functional interface that supplies a {@link PlacementComparator} instance.
 *
 * <p>Used as the factory type in {@link DefaultPlacementComparatorFactory} registry entries
 * and dimension entries. Unlike {@code Function<PlacementComparator, AbstractChainedPlacementComparator>},
 * this supplier does not accept a {@code next} argument — chaining is handled separately via
 * instanceof check in the build process.
 *
 * @see DefaultPlacementComparatorFactory
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
