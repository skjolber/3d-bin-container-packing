package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;

/** Selects a container packing strategy for a packing operation. */
@FunctionalInterface
public interface ContainerStrategyFactory {

	ContainerStrategy create(ContainerItemsCalculator containerItemsCalculator, List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups);
}
