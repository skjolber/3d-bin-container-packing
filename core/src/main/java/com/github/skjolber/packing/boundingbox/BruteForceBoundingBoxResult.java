package com.github.skjolber.packing.boundingbox;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Equally ranked objective winners and the reason the search stopped.
 * Only complete layouts are retained. An unsuccessful result has no winners.
 * An interrupted search retains each objective's best complete layout so far.
 * Collections and layouts are shared without defensive copies or wrappers;
 * callers must not modify them.
 */
public class BruteForceBoundingBoxResult {

	public enum Termination {
		/** Every candidate was visited or safely excluded; no acceptable goal stopped the search. */
		EXHAUSTED,
		/** Every objective has retained a layout satisfying its own goal. These need not be the same layout. */
		GOAL_REACHED,
		/** The deadline or interrupt stopped the search; any returned layout is best-so-far. */
		INTERRUPTED
	}

	protected final Map<String, BoundingBoxLayout> objectiveResults;
	protected final Set<String> reachedGoals;
	protected final Termination termination;
	protected final long duration;

	/**
	 * Retain the supplied collections directly, without copying or wrapping them.
	 * Neither the collections nor their layouts/stacks may be changed after being
	 * passed to this constructor. The map's iteration order is preserved and gives
	 * no objective priority over another.
	 *
	 * @param objectiveResults named complete layouts, possibly sharing layout instances
	 * @param reachedGoals names of objectives whose predicates accepted their layouts
	 * @param termination reason the search stopped
	 * @param duration operation duration in milliseconds
	 */
	protected BruteForceBoundingBoxResult(Map<String, BoundingBoxLayout> objectiveResults, Set<String> reachedGoals, Termination termination, long duration) {
		this.objectiveResults = objectiveResults;
		this.reachedGoals = reachedGoals;
		this.termination = termination;
		this.duration = duration;
	}

	/** All named objective winners in registration order. Do not modify the map or its layouts. */
	public Map<String, BoundingBoxLayout> getObjectiveResults() {
		return objectiveResults;
	}

	/** Names of objectives whose predicates accepted their retained layouts. Do not modify the set. */
	public Set<String> getReachedGoals() {
		return reachedGoals;
	}

	/**
	 * A view of all objective winners in registration order, without copying or deduplication.
	 * Objectives winning the same candidate share a layout, so repeated references are possible.
	 * This is not a history of candidates or a Pareto frontier. Do not modify the view or its layouts.
	 */
	public Collection<BoundingBoxLayout> getResults() {
		return objectiveResults.values();
	}

	public boolean isSuccess() {
		return !objectiveResults.isEmpty();
	}

	public Termination getTermination() {
		return termination;
	}

	/** Wall-clock operation duration in milliseconds, including preparation. */
	public long getDuration() {
		return duration;
	}
}
