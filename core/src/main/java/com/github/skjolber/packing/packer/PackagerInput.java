package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;

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
	protected final boolean insertionOrder;

	/**
	 * @param boxItems box items, or null (or empty) when packing box item groups
	 * @param boxItemGroups box item groups, or null (or empty) when packing box items
	 * @param order the box item order, or null for {@link Order#NONE}
	 */
	public PackagerInput(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containerItems, int maxContainerCount, Order order) {
		this(boxItems, boxItemGroups, containerItems, maxContainerCount, order, true);
	}

	/**
	 * @param boxItems box items, or null (or empty) when packing box item groups
	 * @param boxItemGroups box item groups, or null (or empty) when packing box items
	 * @param order the box item order, or null for {@link Order#NONE}
	 * @param insertionOrder whether to put the placements of results in insertion order (see {@link InsertionSequencer})
	 */
	public PackagerInput(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containerItems, int maxContainerCount, Order order, boolean insertionOrder) {
		this.insertionOrder = insertionOrder;
		this.boxItems = boxItems;
		this.boxItemGroups = boxItemGroups;
		this.containerItems = containerItems;
		this.maxContainerCount = maxContainerCount;
		this.order = order != null ? order : Order.NONE;
	}

	/**
	 * @return the same input with copies of the container items and boxes, so that a session can change their counts
	 */
	public PackagerInput withCopies() {
		List<ContainerItem> containerItemCopies = new ArrayList<>(containerItems.size());
		for(ContainerItem containerItem : containerItems) {
			ContainerItem copy = new ContainerItem(containerItem);
			List<Placement> obstacles = containerItem.getContainer().getObstacles();
			if(!copy.hasInitialPoints() && !obstacles.isEmpty()) {
				// obstacles given with the container: the space around them is free
				copy.setInitialPoints(ObstaclePoints.getFreePoints(containerItem.getContainer(), obstacles));
			}
			containerItemCopies.add(copy);
		}
		if(hasBoxItems()) {
			return new PackagerInput(AbstractPackagerSession.copyBoxItems(boxItems), null, containerItemCopies, maxContainerCount, order, insertionOrder);
		}
		return new PackagerInput(null, AbstractPackagerSession.copyBoxItemGroups(boxItemGroups), containerItemCopies, maxContainerCount, order, insertionOrder);
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

	/**
	 * @return whether to put the placements of results in insertion order (see {@link InsertionSequencer})
	 */
	public boolean isInsertionOrder() {
		return insertionOrder;
	}
}
