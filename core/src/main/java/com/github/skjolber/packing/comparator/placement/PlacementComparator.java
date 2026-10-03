package com.github.skjolber.packing.comparator.placement;

import com.github.skjolber.packing.api.Placement;

/**
 * A typed comparator for {@link Placement} instances.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 *
 * <p>Building comparators: use
 * {@link com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory}
 * for comparators based on load-constraint limits (max weight, pressure, box count,
 * identical-only restriction) and position / physical dimensions (x/y/z, area, volume, weight,
 * support ratio). 
 *
 */
public interface PlacementComparator {

	int compare(Placement a, Placement b);

	/**
	 * Whether this comparator reads {@link Placement#getSupportedArea()}. When it does not, placement
	 * controls can compare a candidate before calculating its support and load, and skip those
	 * calculations for candidates which would not be selected anyway.
	 *
	 * @return true if the comparison depends on the supported area; the default is true, which is always safe
	 */
	default boolean usesSupportedArea() {
		return true;
	}

	/**
	 * Returns a no-op comparator that always returns 0 (all placements are equal).
	 *
	 * @return a singleton no-op comparator
	 */
	static PlacementComparator noOp() {
		return (a, b) -> 0;
	}

}
