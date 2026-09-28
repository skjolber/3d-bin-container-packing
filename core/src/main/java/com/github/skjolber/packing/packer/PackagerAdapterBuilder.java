package com.github.skjolber.packing.packer;

import java.util.List;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

/**
 * 
 * Interface for chaining multiple packagers.
 * 
 */

public interface PackagerAdapterBuilder {

	PackagerAdapterBuilder withBoxItems(List<BoxItem> items);

	PackagerAdapterBuilder withOrder(Order order);

	PackagerAdapterBuilder withInterrupt(PackagerInterruptSupplier interrupt);

	PackagerAdapterBuilder withContainerItemsCalculator(ContainerItemsCalculator calculator);

	PackagerAdapterBuilder withBoxItemGroups(List<BoxItemGroup> items);
	
	PackagerAdapter build();
}
