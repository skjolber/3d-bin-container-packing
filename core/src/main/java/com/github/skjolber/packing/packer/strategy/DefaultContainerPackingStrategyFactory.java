package com.github.skjolber.packing.packer.strategy;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/** Uses cost-aware packing when container costs are present, otherwise input order. */
public class DefaultContainerPackingStrategyFactory implements ContainerPackingStrategyFactory {

	private final OrderedContainerPackingStrategy ordered;
	private final LowestCostContainerPackingStrategy lowestCost;

	public DefaultContainerPackingStrategyFactory(Comparator<IntermediatePackagerResult> comparator,
			Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		this(new OrderedContainerPackingStrategy(comparator, emptyResultSupplier),
				new LowestCostContainerPackingStrategy(comparator));
	}

	public DefaultContainerPackingStrategyFactory(OrderedContainerPackingStrategy ordered,
			LowestCostContainerPackingStrategy lowestCost) {
		this.ordered = Objects.requireNonNull(ordered);
		this.lowestCost = Objects.requireNonNull(lowestCost);
	}

	@Override
	public ContainerPackingStrategy create(boolean hasContainerCost) {
		return hasContainerCost ? lowestCost : ordered;
	}
}
