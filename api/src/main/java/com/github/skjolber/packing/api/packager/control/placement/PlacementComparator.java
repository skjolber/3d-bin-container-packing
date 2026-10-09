package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Placement;

/**
 * A typed comparator for {@link Placement} instances.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 *
 * <p>Building comparators: use
 * {@code com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory}
 * for comparators based on load-constraint limits (max weight, pressure, box count,
 * identical-only restriction) and position / physical dimensions (x/y/z, area, volume, weight,
 * support ratio). 
 *
 */
public interface PlacementComparator {

	/**
	 * @param a a placement
	 * @param b another placement
	 * @return a positive number if {@code a} is preferred, a negative number if {@code b} is preferred, zero if they are equally good
	 */
	int compare(Placement a, Placement b);

	/**
	 * Whether this comparator reads {@link Placement#getSupportedArea()}. When true, the placement controls
	 * compute the supported area of each candidate placement before comparing (a candidate on the floor is
	 * fully supported), whether or not the packager is configured to calculate support. When false (the
	 * default, also for a lambda), the supported area is not computed and reads as zero: a comparator which
	 * reads it must declare it here. Leaving it false lets the controls skip the support calculation.
	 *
	 * @return true if the comparison depends on the supported area; the default is false
	 */
	default boolean usesSupportedArea() {
		return false;
	}

	/**
	 * Whether a placement never compares better with a lower supported area, all else being equal.
	 * When true, placement controls can compare a candidate assuming full support first, and skip
	 * calculating the support of candidates which would not be selected even when fully supported.
	 *
	 * @return true if more support is never worse; the default is false, which is always safe
	 */
	default boolean prefersHigherSupportedArea() {
		return false;
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
