package com.github.skjolber.packing.boundingbox;

import java.util.function.Predicate;

/**
 * An independent named search objective. Negative comparator results mean better.
 * The first valid layout satisfying the goal is retained and this objective stops
 * evaluating candidates. Until then, its best-so-far layout is retained.
 * A null goal optimizes until exhaustion or interruption and prevents early
 * goal termination of the overall search. Callbacks see only valid complete layouts.
 * Objective arguments are validated when registered with a result builder.
 */
public class BoundingBoxObjective {
	protected final String name;
	protected final Predicate<BoundingBox> goal;
	protected final BoundingBoxComparator comparator;

	public BoundingBoxObjective(String name, Predicate<BoundingBox> goal, BoundingBoxComparator comparator) {
		this.name = name;
		this.goal = goal;
		this.comparator = comparator;
	}

	public String name() { return name; }
	public Predicate<BoundingBox> goal() { return goal; }
	public BoundingBoxComparator comparator() { return comparator; }
}
