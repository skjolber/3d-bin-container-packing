package com.github.skjolber.packing.packer.strategy.allocation;

import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Objective;

/**
 * Selects containers from the lowest-cost item-to-container allocation,
 * breaking equal-cost ties in favor of fewer containers.
 */
public final class LowestCostFitContainerPackingStrategy extends AbstractContainerAllocationStrategy {

	public LowestCostFitContainerPackingStrategy() {
		super(Objective.LOWEST_COST);
	}
}
