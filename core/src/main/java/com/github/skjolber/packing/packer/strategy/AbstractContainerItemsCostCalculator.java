package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;

abstract class AbstractContainerItemsCostCalculator implements ContainerItemsCostCalculator {

	static final class CostCapacity {
		final Container container;
		final ContainerCostCalculator calculator;
		final long volume;
		final long weight;
		final long minimumCost;
		final int count;

		CostCapacity(ControlledContainerItem item) {
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

	protected List<CostCapacity> costCapacities(ContainerItemsCalculator containers) {
		List<CostCapacity> capacities = new ArrayList<>(containers.getContainerItemCount());
		for(ControlledContainerItem item : containers.getContainerItems()) {
			if(item.isAvailable()) {
				capacities.add(new CostCapacity(item));
			}
		}
		return capacities;
	}
}
