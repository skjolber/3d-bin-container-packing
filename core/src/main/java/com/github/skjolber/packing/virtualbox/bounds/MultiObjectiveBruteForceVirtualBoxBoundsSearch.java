package com.github.skjolber.packing.virtualbox.bounds;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.virtualbox.VirtualBoxLayout;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult.Termination;

/** A single traversal collecting independent winners for equally ranked objectives. */
public class MultiObjectiveBruteForceVirtualBoxBoundsSearch extends AbstractBruteForceVirtualBoxBoundsSearch {
	protected final VirtualBoxBoundsObjective[] objectives;
	protected final VirtualBoxLayout[] winners;
	protected final boolean[] reached;
	protected final boolean[] improvements;
	protected int remainingGoals;

	public MultiObjectiveBruteForceVirtualBoxBoundsSearch(List<BoxItem> items, Container container, List<VirtualBoxBoundsObjective> objectives,
			PackagerInterruptSupplier interrupt, boolean load) {
		this(items, container, objectives.toArray(VirtualBoxBoundsObjective[]::new), interrupt, load);
	}

	/**
	 * Retain the inventory, container and objective array directly. Do not change
	 * them or their contents after construction or while the search is running.
	 */
	protected MultiObjectiveBruteForceVirtualBoxBoundsSearch(List<BoxItem> items, Container container, VirtualBoxBoundsObjective[] objectives,
			PackagerInterruptSupplier interrupt, boolean load) {
		super(items, container, interrupt, load);
		this.objectives = objectives;
		winners = new VirtualBoxLayout[objectives.length];
		reached = new boolean[objectives.length];
		improvements = new boolean[objectives.length];
		remainingGoals = objectives.length;
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
		VirtualBoxBounds bounds = null;
		boolean anyImproves = false;
		for(int i = 0; i < objectives.length; i++) {
			improvements[i] = false;
			if(reached[i]) {
				continue;
			}
			VirtualBoxBoundsObjective objective = objectives[i];
			if(objective.goal() != null) {
				if(bounds == null) {
					bounds = new VirtualBoxBounds(dx, dy, dz);
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
				bounds = new VirtualBoxBounds(dx, dy, dz);
			}
			// Objectives winning the same candidate share one independent search snapshot.
			VirtualBoxLayout layout = new VirtualBoxLayout(bounds, createSnapshot().getPlacements());
			for(int i = 0; i < objectives.length; i++) {
				if(improvements[i]) {
					winners[i] = layout;
				}
			}
		}
		return remainingGoals == 0;
	}

	@Override
	protected VirtualBoxBoundsResult result(Termination termination, long duration) {
		Map<String, VirtualBoxLayout> results = Map.of();
		Set<String> goals = Set.of();
		for(int i = 0; i < objectives.length; i++) {
			if(winners[i] != null) {
				if(results.isEmpty()) {
					results = new LinkedHashMap<>(objectives.length);
				}
				results.put(objectives[i].name(), winners[i]);
			}
			if(reached[i]) {
				if(goals.isEmpty()) {
					goals = new LinkedHashSet<>(objectives.length);
				}
				goals.add(objectives[i].name());
			}
		}
		return new VirtualBoxBoundsResult(results, goals, termination, duration);
	}
}
