package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement;

/**
 * A typed comparator for {@link Placement} instances.
 *
 * <p>Convention: {@code compare(a, b) > 0} means {@code a} is the preferred placement.
 *
 * <p>This interface extends both {@link Comparator}&lt;{@link Placement}&gt; and
 * {@link com.github.skjolber.packing.api.packager.control.placement.PlacementComparator}, making
 * it a drop-in replacement wherever either of those types is expected.
 *
 * <h3>Building comparators</h3>
 * <ul>
 *   <li>{@link PlacementComparatorBuilder} — comparators based on load-constraint limits
 *       (max weight, max pressure, max box-count, identical-only restriction).</li>
 *   <li>{@link PlacementComparatorBuilder} — comparators based on position and physical
 *       dimensions (x/y/z coordinates, footprint area, volume, box weight, support ratio).</li>
 *   <li>{@link CompositePlacementComparator} — combines two {@code PlacementComparator}
 *       instances, evaluating the second only when the first returns zero.</li>
 * </ul>
 *
 * <p>To wrap an existing {@link Comparator}&lt;{@link Placement}&gt; use {@link #of(Comparator)}.
 */
public interface PlacementComparator {

	int compare(Placement a, Placement b);
	
	/**
	 * Returns a composite comparator that evaluates this comparator first, then {@code other}
	 * when the result is zero.
	 *
	 * @param other the comparator to apply on a tie; must not be {@code null}
	 * @return a new composite {@link PlacementComparator}
	 */
	default PlacementComparator thenComparing(PlacementComparator other) {
		return new CompositePlacementComparator(this, other);
	}

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
