package com.github.skjolber.packing.packer.strategy.cost;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;

abstract class AbstractContainerItemsCostCalculator implements ContainerItemsCostCalculator {

	static final class CostCapacity {
		final Container container;
		final ContainerCostCalculator calculator;
		final long volume;
		final long weight;
		final long minimumCost;
		final int count;

		CostCapacity(ContainerItem item) {
			container = item.getContainer();
			calculator = item.getCostCalculator();
			if(calculator == null) {
				throw new IllegalStateException("Missing cost calculator for container index " + item.getIndex());
			}
			volume = container.getMaxLoadVolume();
			weight = container.getMaxLoadWeight();
			minimumCost = calculator.getMinimumCost();
			if(minimumCost < 0) {
				throw new IllegalStateException("Negative minimum cost for container index " + item.getIndex());
			}
			count = item.getCount();
		}
	}

	/**
	 * Orders capacities; a custom interface rather than {@link java.util.Comparator}. A negative number means the first
	 * capacity sorts first, as for the other comparators which sort (for example {@code Point2DComparator}).
	 */
	@FunctionalInterface
	interface CostCapacityComparator {

		int compare(CostCapacity first, CostCapacity second);
	}

	/**
	 * Stable insertion sort of a copy of the capacities: there are as many capacities as container types, so only a few.
	 */
	static CostCapacity[] sorted(List<CostCapacity> capacities, CostCapacityComparator comparator) {
		CostCapacity[] sorted = capacities.toArray(new CostCapacity[0]);
		for(int i = 1; i < sorted.length; i++) {
			CostCapacity capacity = sorted[i];
			int j = i - 1;
			// move past the capacities which sort strictly after, so that equal capacities keep their order
			while(j >= 0 && comparator.compare(sorted[j], capacity) > 0) {
				sorted[j + 1] = sorted[j];
				j--;
			}
			sorted[j + 1] = capacity;
		}
		return sorted;
	}

	protected List<CostCapacity> costCapacities(ContainerInventory containers) {
		List<CostCapacity> capacities = new ArrayList<>(containers.getContainerItemCount());
		for(ContainerItem item : containers.getContainerItems()) {
			if(item.isAvailable()) {
				capacities.add(new CostCapacity(item));
			}
		}
		return capacities;
	}
}
