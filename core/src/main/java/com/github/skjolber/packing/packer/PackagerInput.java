package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;

/**
 * The input of a packaging operation: either box items or box item groups, the available containers, the
 * max number of containers and the box order. Packagers create sessions for an input with
 * {@link AbstractPackager#createSession(PackagerInput, com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier)}.
 */
public class PackagerInput {

	protected final List<BoxItem> boxItems;
	protected final List<BoxItemGroup> boxItemGroups;
	protected final List<ContainerItem> containerItems;
	protected final int maxContainerCount;
	protected final Order order;

	/**
	 * @param boxItems box items, or null (or empty) when packing box item groups
	 * @param boxItemGroups box item groups, or null (or empty) when packing box items
	 */
	public PackagerInput(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containerItems, int maxContainerCount, Order order) {
		this.boxItems = boxItems;
		this.boxItemGroups = boxItemGroups;
		this.containerItems = containerItems;
		this.maxContainerCount = maxContainerCount;
		this.order = order;
	}

	/** @return true if packing box items, false if packing box item groups */
	public boolean hasBoxItems() {
		return boxItems != null && !boxItems.isEmpty();
	}

	public List<BoxItem> getBoxItems() {
		return boxItems;
	}

	public List<BoxItemGroup> getBoxItemGroups() {
		return boxItemGroups;
	}

	public List<ContainerItem> getContainerItems() {
		return containerItems;
	}

	public int getMaxContainerCount() {
		return maxContainerCount;
	}

	public Order getOrder() {
		return order;
	}
}
