package com.github.skjolber.packing.boundingbox;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

/** Specialized single-objective search, without per-candidate objective arrays or loops. */
public class SingleObjectiveBruteForceBoundingBoxSearch extends AbstractBruteForceBoundingBoxSearch {
	protected final BoundingBoxObjective objective;
	protected final BoundingBoxComparator comparator;
	protected final Predicate<BoundingBox> goal;
	protected BoundingBox bestBounds;
	protected BoundingBoxLayout bestLayout;

	public SingleObjectiveBruteForceBoundingBoxSearch(List<BoxItem> items, Container container, BoundingBoxObjective objective,
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
		BoundingBox bounds = new BoundingBox(dx, dy, dz);
		boolean acceptable = goal != null && goal.test(bounds);
		if(acceptable || bestBounds == null || goal == null || comparator.compare(bounds, bestBounds) < 0) {
			bestBounds = bounds;
			// Snapshot only an improvement or accepted goal, never the ordinary search nodes.
			bestLayout = new BoundingBoxLayout(bounds, createSnapshot());
		}
		return acceptable;
	}


	@Override
	protected BruteForceBoundingBoxResult result(Termination termination, long duration) {
		return new BruteForceBoundingBoxResult(bestLayout == null ? Map.of() : Map.of(objective.name(), bestLayout),
				termination == Termination.GOAL_REACHED ? Set.of(objective.name()) : Set.of(), termination, duration);
	}
}
