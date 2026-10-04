package com.github.skjolber.packing.api.packager.strategy;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

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
	ContainerStrategy create(ContainerInventory inventory, List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups);
}
