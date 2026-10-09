package com.github.skjolber.packing.packer.strategy;

import java.util.List;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategy;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.packer.strategy.cost.LowestCostContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.ordered.OrderedContainerPackingStrategy;

/**
 * Uses cost-aware packing when container costs are present, otherwise input
 * order. For unconstrained inventory it selects an ordered variant which does
 * not repeat allocation planning before every attempted container.
 * <p>
 * Stateless: every call creates a new strategy from the arguments, so the factory can be shared freely.
 */
public class DefaultContainerPackingStrategyFactory implements ContainerPackingStrategyFactory {

	@Override
	public ContainerPackingStrategy create(ContainerInventory containerInventory, List<BoxItem> remainingBoxItems, List<BoxItemGroup> boxItemGroups,
			IntermediatePackagerResultComparator comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		if(containerInventory.hasCost()) {
			return new LowestCostContainerPackingStrategy(comparator);
		}
		OrderedContainerPackingStrategy ordered = new OrderedContainerPackingStrategy(comparator, emptyResultSupplier);
		if(isAllocationAlwaysFeasible(containerInventory, remainingBoxItems, boxItemGroups)) {
			return ordered.withoutAllocationFeasibilityCheck();
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
	private static boolean isAllocationAlwaysFeasible(ContainerInventory calculator, List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups) {
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
