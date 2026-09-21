package com.github.skjolber.packing.packer.strategy;

import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCostCalculator;
import com.github.skjolber.packing.packer.EstimatingContainerItemsCostCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.strategy.BruteForceContainerStrategy.Controls;

/**
 * Selects the lowest-cost complete packing, breaking ties in favor of fewer
 * containers. Branches are skipped when the minimum possible additional cost
 * cannot improve the best result.
 */
public final class LowestCostControls implements Controls {

	private final ContainerItemsCostCalculator costCalculator;
	private ContainerResult best;

	public LowestCostControls() {
		this(new EstimatingContainerItemsCostCalculator());
	}

	public LowestCostControls(ContainerItemsCostCalculator costCalculator) {
		this.costCalculator = Objects.requireNonNull(costCalculator);
	}

	@Override
	public boolean attempt(List<Container> containers, PackagerAdapter state, List<Integer> availableContainerIndexes, int selectedContainerIndex) {
		if(best == null) {
			return true;
		}

		long selectedMinimumCost = getMinimumCost(state.getContainerItem(selectedContainerIndex), selectedContainerIndex);
		long estimatedMinimumCost = estimateMinimumCost(state);
		long minimumAdditionalCost = Math.max(selectedMinimumCost, estimatedMinimumCost);
		return canImprove(state.getContainerItemsCalculator().getCost(), minimumAdditionalCost,
				containers.size() + 1, best.getCost(), best.getPackList().size());
	}

	@Override
	public boolean result(ContainerResult result) {
		if(result.getCost() < 0) {
			throw new IllegalStateException("Container cost must be non-negative");
		}
		if(best == null || result.getCost() < best.getCost()
				|| result.getCost() == best.getCost() && result.getPackList().size() < best.getPackList().size()) {
			best = result;
		}
		return true;
	}

	@Override
	public ContainerResult getResult() {
		return best;
	}

	private long estimateMinimumCost(PackagerAdapter state) {
		ContainerItemsCalculator containers = state.getContainerItemsCalculator();
		int maxCount = state.getMaxContainerCount();
		List<BoxItemGroup> groups = state.getRemainingBoxItemGroups();
		if(groups != null) {
			return costCalculator.getGroupMinimumCost(containers, groups, maxCount);
		}
		return costCalculator.getMinimumCost(containers, state.getRemainingBoxItems(), maxCount);
	}

	private static long getMinimumCost(ContainerItem item, int index) {
		ContainerCostCalculator calculator = item.getCostCalculator();
		if(calculator == null) {
			throw new IllegalStateException("Missing cost calculator for container index " + index);
		}
		long cost = calculator.getMinimumCost();
		if(cost < 0) {
			throw new IllegalStateException("Minimum container cost must be non-negative for index " + index);
		}
		return cost;
	}

	private static boolean canImprove(long currentCost, long minimumAdditionalCost, int minimumContainers, long bestCost, int bestCount) {
		if(currentCost > bestCost) {
			return false;
		}
		long headroom = bestCost - currentCost;
		return minimumAdditionalCost < headroom
				|| minimumAdditionalCost == headroom && minimumContainers < bestCount;
	}
}
