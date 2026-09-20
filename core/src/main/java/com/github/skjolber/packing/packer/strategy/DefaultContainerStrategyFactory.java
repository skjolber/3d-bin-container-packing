package com.github.skjolber.packing.packer.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/** Uses cost-aware packing when container costs are present, otherwise input order. */
public class DefaultContainerStrategyFactory implements ContainerStrategyFactory {

	private final OrderedContainerPackingStrategy ordered;
	private final LowestCostContainerPackingStrategy lowestCost;

	public DefaultContainerStrategyFactory(Comparator<IntermediatePackagerResult> comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		this(new OrderedContainerPackingStrategy(comparator, emptyResultSupplier), new LowestCostContainerPackingStrategy(comparator));
	}

	public DefaultContainerStrategyFactory(OrderedContainerPackingStrategy ordered, LowestCostContainerPackingStrategy lowestCost) {
		this.ordered = Objects.requireNonNull(ordered);
		this.lowestCost = Objects.requireNonNull(lowestCost);
	}

	@Override
	public ContainerStrategy create(ContainerItemsCalculator containerItemsCalculator, List<BoxItem> remainingBoxItems, List<BoxItemGroup> boxItemGroups) {
		if(containerItemsCalculator.hasCost()) {
			return lowestCost;
		}
		return ordered;
	}
}
