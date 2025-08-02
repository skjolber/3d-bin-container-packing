package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.packager.ControlContainerItem;

public class ControlContainerItemsCalculator extends AbstractContainerItemsCalculator<ControlContainerItem> {

	protected final List<ControlContainerItem> containerItems;

	public ControlContainerItemsCalculator(List<ControlContainerItem> items) {
		this.containerItems = items;

		calculateMaxLoadVolume();
		calculateMaxLoadWeight();
	}

	public int getContainerItemCount() {
		return containerItems.size();
	}
	
	public ControlContainerItem getContainerItem(int index) {
		return containerItems.get(index);
	}

}
