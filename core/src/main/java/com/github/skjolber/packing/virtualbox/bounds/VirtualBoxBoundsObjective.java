package com.github.skjolber.packing.virtualbox.bounds;

import java.util.function.Predicate;

/**
 * An independent named search objective. Negative comparator results mean better.
 * The first valid layout satisfying the goal is retained and this objective stops
 * evaluating candidates. Until then, its best-so-far layout is retained.
 * A null goal optimizes until exhaustion or interruption and prevents early
 * goal termination of the overall search. Callbacks see only valid complete layouts.
 * Objective arguments are validated when registered with a result builder.
 */
public class VirtualBoxBoundsObjective {
	protected final String name;
	protected final Predicate<VirtualBoxBounds> goal;
	protected final VirtualBoxBoundsComparator comparator;

	public VirtualBoxBoundsObjective(String name, Predicate<VirtualBoxBounds> goal, VirtualBoxBoundsComparator comparator) {
		this.name = name;
		this.goal = goal;
		this.comparator = comparator;
	}

	public String name() { return name; }
	public Predicate<VirtualBoxBounds> goal() { return goal; }
	public VirtualBoxBoundsComparator comparator() { return comparator; }
}
