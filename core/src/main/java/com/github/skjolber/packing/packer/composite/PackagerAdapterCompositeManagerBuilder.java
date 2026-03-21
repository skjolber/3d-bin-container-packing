package com.github.skjolber.packing.packer.composite;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;

public interface PackagerAdapterCompositeManagerBuilder<B extends PackagerAdapterCompositeManagerBuilder<B>> {
	
	B withBoxItems(List<BoxItem> boxItem);
	B withBoxItemGroups(List<BoxItemGroup> items);
	B withOrder(Order order);
	B withContainerItemsCalculator(ContainerItemsCalculator packagerContainerItems);
	B withPackagerAdapters(List<PackagerAdapter> adapters);
	
	PackagerAdapterCompositeManager build();
	
}
