package com.github.skjolber.packing.boundingbox;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.skjolber.packing.api.Stack;

/**
 * The primary assembly, any requested additional objective winners, and the
 * reason the search stopped. Only complete layouts are retained. An unsuccessful
 * result has an empty stack, no bounding box and empty result collections.
 * An interrupted search retains each objective's best complete layout so far.
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
	protected final Stack stack;
	protected final BoundingBox boundingBox;
	protected final Termination termination;
	protected final long duration;
	protected final Map<String, BoundingBoxLayout> additionalResults;
	protected final List<BoundingBoxLayout> results;

	protected BruteForceBoundingBoxResult(Map<String, BoundingBoxLayout> objectiveResults, Set<String> reachedGoals, Termination termination, long duration) {
		this.objectiveResults = Collections.unmodifiableMap(new LinkedHashMap<>(objectiveResults));
		this.reachedGoals = Collections.unmodifiableSet(new LinkedHashSet<>(reachedGoals));
		Map<String, BoundingBoxLayout> additionalResults = new LinkedHashMap<>(objectiveResults);
		BoundingBoxLayout primary = null;
		if(!additionalResults.isEmpty()) {
			String first = additionalResults.keySet().iterator().next();
			primary = additionalResults.remove(first);
		}
		this.stack = primary == null ? new Stack() : primary.getStack();
		this.boundingBox = primary == null ? null : primary.getBoundingBox();
		this.termination = termination;
		this.duration = duration;
		this.additionalResults = Collections.unmodifiableMap(new LinkedHashMap<>(additionalResults));
		LinkedHashSet<BoundingBoxLayout> layouts = new LinkedHashSet<>();
		if(primary != null) {
			layouts.add(primary);
		}
		layouts.addAll(additionalResults.values());
		this.results = List.copyOf(layouts);
	}

	/** All named objective winners in registration order, including the first objective. */
	public Map<String, BoundingBoxLayout> getObjectiveResults() {
		return objectiveResults;
	}

	/** Names of objectives whose own predicates have accepted their retained layouts. */
	public Set<String> getReachedGoals() {
		return reachedGoals;
	}

	/**
	 * Named objective winners, in registration order. Empty when no complete layout
	 * was found. Winners are best-so-far when a goal or interruption ends the search.
	 * The map is unmodifiable; objectives can share the same layout instance.
	 */
	public Map<String, BoundingBoxLayout> getAdditionalResults() {
		return additionalResults;
	}

	/**
	 * Primary result followed by additional winners, omitting repeated references
	 * to the same retained layout. Different layouts can have identical bounds.
	 * This is not a history of candidates or a Pareto frontier.
	 */
	public List<BoundingBoxLayout> getResults() {
		return results;
	}

	/** Primary placements, independent of search state but possibly shared with an additional winner. */
	public Stack getStack() {
		return stack;
	}

	/** @return the primary envelope, or {@code null} if no complete arrangement was found */
	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public boolean isSuccess() {
		return boundingBox != null;
	}

	public Termination getTermination() {
		return termination;
	}

	/** Wall-clock operation duration in milliseconds, including preparation. */
	public long getDuration() {
		return duration;
	}
}
