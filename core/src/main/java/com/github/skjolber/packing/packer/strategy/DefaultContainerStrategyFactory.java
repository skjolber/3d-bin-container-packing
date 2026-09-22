package com.github.skjolber.packing.packer.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/**
 * Uses cost-aware packing when container costs are present, otherwise input
 * order. For unconstrained inventory it selects an ordered variant which does
 * not repeat allocation planning before every attempted container.
 */
public class DefaultContainerStrategyFactory implements ContainerStrategyFactory {

	private final OrderedContainerPackingStrategy ordered;
	private final OrderedContainerPackingStrategy orderedWithoutAllocationFeasibilityCheck;
	private final LowestCostContainerPackingStrategy lowestCost;

	public DefaultContainerStrategyFactory(Comparator<IntermediatePackagerResult> comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		this(new OrderedContainerPackingStrategy(comparator, emptyResultSupplier), new LowestCostContainerPackingStrategy(comparator));
	}

	public DefaultContainerStrategyFactory(OrderedContainerPackingStrategy ordered, LowestCostContainerPackingStrategy lowestCost) {
		this.ordered = Objects.requireNonNull(ordered);
		this.orderedWithoutAllocationFeasibilityCheck = ordered.withoutAllocationFeasibilityCheck();
		this.lowestCost = Objects.requireNonNull(lowestCost);
	}

	@Override
	public ContainerStrategy create(ContainerItemsCalculator containerItemsCalculator, List<BoxItem> remainingBoxItems, List<BoxItemGroup> boxItemGroups) {
		if(containerItemsCalculator.hasCost()) {
			return lowestCost;
		}
		if(isAllocationAlwaysFeasible(containerItemsCalculator, remainingBoxItems, boxItemGroups)) {
			return orderedWithoutAllocationFeasibilityCheck;
		}
		return ordered;
	}

	/**
	 * Determine whether one compatible container can always be reserved for each
	 * remaining unit. Every available type must fit every unit and contain at
	 * least {@code unitCount} instances. As accepting a container removes at
	 * least one unit, both the per-type inventory and total container limit keep
	 * this invariant for the rest of the operation.
	 */
	private static boolean isAllocationAlwaysFeasible(ContainerItemsCalculator calculator, List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups) {
		int unitCount;
		if(boxItemGroups != null) {
			unitCount = boxItemGroups.size();
		} else if(boxItems != null) {
			unitCount = 0;
			for(BoxItem boxItem : boxItems) {
				unitCount += boxItem.getCount();
			}
		} else {
			return false;
		}
		if(unitCount == 0 || unitCount > calculator.getContainerCount()) {
			return false;
		}

		boolean available = false;
		for(int i = 0; i < calculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = calculator.getContainerItem(i);
			if(!containerItem.isAvailable()) {
				continue;
			}
			if(containerItem.getCount() < unitCount) {
				return false;
			}
			available = true;
			if(boxItemGroups != null) {
				for(BoxItemGroup group : boxItemGroups) {
					if(!calculator.canLoad(group, i)) {
						return false;
					}
				}
			} else {
				for(BoxItem boxItem : boxItems) {
					if(!calculator.canLoad(boxItem, i)) {
						return false;
					}
				}
			}
		}
		return available;
	}
}
