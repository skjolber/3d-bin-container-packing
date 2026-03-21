package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

/**
 * 
 * Interface for combining multiple packagers.
 * 
 */

public interface PackagerAdapterFactory {

	PackagerAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups, ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt);

	PackagerAdapter createBoxItemAdapter(List<BoxItem> items, ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt);

}
