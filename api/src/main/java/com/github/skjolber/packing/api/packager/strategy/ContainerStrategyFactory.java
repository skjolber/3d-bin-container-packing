package com.github.skjolber.packing.api.packager.strategy;

import java.util.List;

import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

/**
 * Selects a container strategy for a packaging operation. Configure with the packager builders'
 * {@code withContainerStrategyFactory(..)}.
 */
@FunctionalInterface
public interface ContainerStrategyFactory {

	/**
	 * @param inventory the available containers
	 * @param boxItems the box items to pack, or null when packing box item groups
	 * @param boxItemGroups the box item groups to pack, or null when packing box items
	 * @return the strategy for this packaging operation
	 */
	ContainerStrategy create(ContainerInventory inventory, List<RemainingBoxItem> boxItems, List<RemainingBoxItemGroup> boxItemGroups);
}
