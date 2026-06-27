package com.github.skjolber.packing.comparator.placement;

import java.util.Comparator;

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
 * <p>To wrap an existing {@link Comparator}&lt;{@link Placement}&gt; use {@link #of(Comparator)}.
 */
public interface PlacementComparator {

	int compare(Placement a, Placement b);

	/**
	 * Returns a no-op comparator that always returns 0 (all placements are equal).
	 *
	 * @return a singleton no-op comparator
	 */
	static PlacementComparator noOp() {
		return (a, b) -> 0;
	}

	/**
	 * Wraps an arbitrary {@link Comparator}&lt;{@link Placement}&gt; as a {@link PlacementComparator}.
	 *
	 * <p>If the argument already implements {@link PlacementComparator} it is returned as-is.
	 *
	 * @param comparator the comparator to wrap; must not be {@code null}
	 * @return a {@link PlacementComparator} delegating to {@code comparator}
	 */
	static PlacementComparator of(Comparator<Placement> comparator) {
		if (comparator instanceof PlacementComparator) {
			return (PlacementComparator) comparator;
		}
		return comparator::compare;
	}
}
