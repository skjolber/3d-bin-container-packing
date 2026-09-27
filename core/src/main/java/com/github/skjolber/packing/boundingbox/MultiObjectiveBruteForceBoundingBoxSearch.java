package com.github.skjolber.packing.boundingbox;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

/** A single traversal collecting independent winners for equally ranked objectives. */
public class MultiObjectiveBruteForceBoundingBoxSearch extends AbstractBruteForceBoundingBoxSearch {
	protected final BoundingBoxObjective[] objectives;
	protected final BoundingBoxLayout[] winners;
	protected final boolean[] reached;
	protected final boolean[] improvements;
	protected int remainingGoals;

	public MultiObjectiveBruteForceBoundingBoxSearch(List<BoxItem> items, Container container, List<BoundingBoxObjective> objectives,
			PackagerInterruptSupplier interrupt, boolean load) {
		super(items, container, interrupt, load);
		this.objectives = objectives.toArray(BoundingBoxObjective[]::new);
		winners = new BoundingBoxLayout[objectives.size()];
		reached = new boolean[objectives.size()];
		improvements = new boolean[objectives.size()];
		remainingGoals = objectives.size();
	}

	@Override
	protected boolean cannotImprove(int dx, int dy, int dz) {
		for(int i = 0; i < objectives.length; i++) {
			if(reached[i]) {
				continue;
			}
			// An arbitrary predicate can accept a comparator-worse layout.
			if(objectives[i].goal() != null || winners[i] == null) {
				return false;
			}
			if(objectives[i].comparator().canImprove(dx, dy, dz, winners[i].getBoundingBox())) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean complete(int dx, int dy, int dz) throws PackagerInterruptedException {
		// Validate before calling user code, including predicates for non-improving candidates.
		if(!isValidLayout()) {
			return false;
		}
		BoundingBox bounds = null;
		boolean anyImproves = false;
		for(int i = 0; i < objectives.length; i++) {
			improvements[i] = false;
			if(reached[i]) {
				continue;
			}
			BoundingBoxObjective objective = objectives[i];
			if(objective.goal() != null) {
				if(bounds == null) {
					bounds = new BoundingBox(dx, dy, dz);
				}
				if(objective.goal().test(bounds)) {
					reached[i] = true;
					remainingGoals--;
				}
			}
			boolean improves = reached[i] || winners[i] == null;
			if(!improves) {
				improves = bounds == null
						? objective.comparator().compare(dx, dy, dz, winners[i].getBoundingBox()) < 0
						: objective.comparator().compare(bounds, winners[i].getBoundingBox()) < 0;
			}
			improvements[i] = improves;
			anyImproves |= improves;
		}
		if(anyImproves) {
			if(bounds == null) {
				bounds = new BoundingBox(dx, dy, dz);
			}
			// Objectives winning the same candidate share one immutable search snapshot.
			BoundingBoxLayout layout = new BoundingBoxLayout(bounds, createSnapshot());
			for(int i = 0; i < objectives.length; i++) {
				if(improvements[i]) {
					winners[i] = layout;
				}
			}
		}
		return remainingGoals == 0;
	}

	@Override
	protected BruteForceBoundingBoxResult result(Termination termination, long duration) {
		Map<String, BoundingBoxLayout> results = new LinkedHashMap<>();
		Set<String> goals = new LinkedHashSet<>();
		for(int i = 0; i < objectives.length; i++) {
			if(winners[i] != null) {
				results.put(objectives[i].name(), winners[i]);
			}
			if(reached[i]) {
				goals.add(objectives[i].name());
			}
		}
		return new BruteForceBoundingBoxResult(results, goals, termination, duration);
	}
}
