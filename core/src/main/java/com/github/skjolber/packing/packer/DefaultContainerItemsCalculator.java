package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;

public class DefaultContainerItemsCalculator extends AbstractContainerItemsCalculator<ContainerItem> {

	protected final List<ContainerItem> containerItems;

	public DefaultContainerItemsCalculator(List<ContainerItem> items) {
		this.containerItems = items;

		calculateMaxLoadVolume();
		calculateMaxLoadWeight();
	}

	public int getContainerItemCount() {
		return containerItems.size();
	}
	
	public ContainerItem getContainerItem(int index) {
		return containerItems.get(index);
	}
	
}
