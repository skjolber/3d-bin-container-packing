package com.github.skjolber.packing.packer.strategy.allocation;

import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Objective;

/**
 * Selects containers from an item-to-container allocation using the fewest
 * possible containers under individual-fit, volume and weight constraints.
 */
public final class FewestContainersFitContainerPackingStrategy extends AbstractContainerAllocationStrategy {

	public FewestContainersFitContainerPackingStrategy() {
		super(Objective.FEWEST_CONTAINERS);
	}
}
