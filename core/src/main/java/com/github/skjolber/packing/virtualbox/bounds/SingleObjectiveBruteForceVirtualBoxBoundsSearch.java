package com.github.skjolber.packing.virtualbox.bounds;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.virtualbox.VirtualBoxLayout;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult.Termination;

/** Specialized single-objective search, without per-candidate objective arrays or loops. */
public class SingleObjectiveBruteForceVirtualBoxBoundsSearch extends AbstractBruteForceVirtualBoxBoundsSearch {
	protected final VirtualBoxBoundsObjective objective;
	protected final VirtualBoxBoundsComparator comparator;
	protected final Predicate<VirtualBoxBounds> goal;
	protected VirtualBoxBounds bestBounds;
	protected VirtualBoxLayout bestLayout;

	public SingleObjectiveBruteForceVirtualBoxBoundsSearch(List<BoxItem> items, Container container, VirtualBoxBoundsObjective objective,
			PackagerInterruptSupplier interrupt, boolean load) {
		super(items, container, interrupt, load);
		this.objective = objective;
		this.comparator = objective.comparator();
		this.goal = objective.goal();
	}

	@Override
	protected boolean cannotImprove(int dx, int dy, int dz) {
		return goal == null && bestBounds != null && !comparator.canImprove(dx, dy, dz, bestBounds);
	}

	@Override
	protected boolean complete(int dx, int dy, int dz) throws PackagerInterruptedException {
		if(!isValidLayout()) {
			return false;
		}
		if(goal == null && bestBounds != null && comparator.compare(dx, dy, dz, bestBounds) >= 0) {
			return false;
		}
		VirtualBoxBounds bounds = new VirtualBoxBounds(dx, dy, dz);
		boolean acceptable = goal != null && goal.test(bounds);
		if(acceptable || bestBounds == null || goal == null || comparator.compare(bounds, bestBounds) < 0) {
			bestBounds = bounds;
			// Snapshot only an improvement or accepted goal, never the ordinary search nodes.
			bestLayout = new VirtualBoxLayout(bounds, createSnapshot().getPlacements());
		}
		return acceptable;
	}


	@Override
	protected VirtualBoxBoundsResult result(Termination termination, long duration) {
		return new VirtualBoxBoundsResult(bestLayout == null ? Map.of() : Map.of(objective.name(), bestLayout),
				termination == Termination.GOAL_REACHED ? Set.of(objective.name()) : Set.of(), termination, duration);
	}
}
