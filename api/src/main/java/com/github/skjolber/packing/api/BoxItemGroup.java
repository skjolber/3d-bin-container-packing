package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Items which belong together, for example different parts of a single product
 * or order.
 * <p>
 * Groups are the input of packaging: packing does not modify them. A box item refers to the group it was added to
 * (see {@link BoxItem#getGroup()}).
 *
 */

public class BoxItemGroup {

	protected String id;
	protected final List<BoxItem> items;

	protected int containerPriority;
	protected int extractionOrder;

	public BoxItemGroup(String id, List<BoxItem> items) {
		super();
		this.id = id;
		this.items = items;
		for (BoxItem boxItem : items) {
			boxItem.setGroup(this);
		}
	}

	/**
	 * Set the container priority of the group and its box items, see {@link BoxItem#withContainerPriority(int)}.
	 *
	 * @param containerPriority priority, lower values in earlier containers
	 * @return this group
	 */
	public BoxItemGroup withContainerPriority(int containerPriority) {
		this.containerPriority = containerPriority;
		for (BoxItem boxItem : items) {
			boxItem.withContainerPriority(containerPriority);
		}
		return this;
	}

	/**
	 * Set the extraction order of the group and its box items, see {@link BoxItem#withExtractionOrder(int)}. Groups
	 * with different extraction orders are not interleaved.
	 *
	 * @param extractionOrder order, lower values extracted first
	 * @return this group
	 */
	public BoxItemGroup withExtractionOrder(int extractionOrder) {
		this.extractionOrder = extractionOrder;
		for (BoxItem boxItem : items) {
			boxItem.withExtractionOrder(extractionOrder);
		}
		return this;
	}

	/**
	 * Copy the container priority and extraction order of another group (not to the box items).
	 *
	 * @param other the group to copy from
	 * @return this group
	 */
	public BoxItemGroup withOrderingOf(BoxItemGroup other) {
		this.containerPriority = other.containerPriority;
		this.extractionOrder = other.extractionOrder;
		return this;
	}

	public int getContainerPriority() {
		return containerPriority;
	}

	public int getExtractionOrder() {
		return extractionOrder;
	}

	public String getId() {
		return id;
	}

	public List<BoxItem> getItems() {
		return items;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int size() {
		return items.size();
	}

	public BoxItem get(int i) {
		return items.get(i);
	}

	/**
	 * @return the number of boxes
	 */
	public int getBoxCount() {
		int count = 0;
		for (BoxItem boxItem : items) {
			count += boxItem.getCount();
		}
		return count;
	}

	/**
	 * @return true if the group has no boxes
	 */
	public boolean isEmpty() {
		for (BoxItem boxItem : items) {
			if (boxItem.getCount() > 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return a group with copies of the box items
	 */
	public BoxItemGroup copy() {
		List<BoxItem> items = new ArrayList<>();
		for (BoxItem boxItem : this.items) {
			items.add(boxItem.copy());
		}
		return new BoxItemGroup(id, items).withOrderingOf(this);
	}

	public long getVolume() {
		long volume = 0;
		for (BoxItem boxItem : items) {
			volume += boxItem.getVolume();
		}
		return volume;
	}

	public long getWeight() {
		long weight = 0;
		for (BoxItem boxItem : items) {
			weight += boxItem.getWeight();
		}
		return weight;
	}

	@Override
	public String toString() {
		return id;
	}
}
