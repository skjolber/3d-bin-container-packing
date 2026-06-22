package com.github.skjolber.packing.comparator;

import java.util.Objects;

import com.github.skjolber.packing.api.Placement;

/**
 * A {@link PlacementComparator} that evaluates two comparators in sequence.
 *
 * <p>The {@link #first} comparator is evaluated first. If it returns a non-zero result that
 * result is returned. Otherwise {@link #second} is evaluated. This makes it possible to combine a
 * constraint-based comparator with a position-based comparator:
 *
 * <pre>
 * PlacementComparator constraint = PlacementComparatorBuilder.newBuilder()
 *         .noIdenticalConstraintIsBetter()
 *         .higherMaxLoadWeightIsBetter()
 *         .build();
 *
 * PlacementComparator position = PlacementComparatorBuilder.newBuilder()
 *         .lowerZIsBetter()
 *         .higherAreaIsBetter()
 *         .build();
 *
 * PlacementComparator combined = new CompositePlacementComparator(constraint, position);
 * // or equivalently:
 * PlacementComparator combined = constraint.thenComparing(position);
 * </pre>
 *
 * <p>Both {@code first} and {@code second} must be non-null. The comparator may be nested
 * arbitrarily deep by passing another {@code CompositePlacementComparator} as either argument.
 */
public final class CompositePlacementComparator implements PlacementComparator {

	private final PlacementComparator first;
	private final PlacementComparator second;

	/**
	 * Constructs a composite comparator.
	 *
	 * @param first  evaluated first; must not be {@code null}
	 * @param second evaluated when {@code first} returns 0; must not be {@code null}
	 */
	public CompositePlacementComparator(PlacementComparator first, PlacementComparator second) {
		this.first = Objects.requireNonNull(first, "first");
		this.second = Objects.requireNonNull(second, "second");
	}

	@Override
	public int compare(Placement a, Placement b) {
		int r = first.compare(a, b);
		return r != 0 ? r : second.compare(a, b);
	}

	/** Returns the first comparator. */
	public PlacementComparator getFirst() {
		return first;
	}

	/** Returns the second comparator. */
	public PlacementComparator getSecond() {
		return second;
	}
}
