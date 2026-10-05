package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Items which belong together, for example different parts of a single product
 * or order.
 * 
 */

public class BoxItemGroup {

	protected String id;

	protected List<BoxItem> items;

	protected int index = -1;

	protected List<BoxItem> resetItems;

	protected int containerPriority;
	protected int extractionOrder;

	public BoxItemGroup(String id, List<BoxItem> items, int index) {
		this(id, items);
		this.index = index;
	}

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

	public BoxItemGroup(BoxItemGroup copy) {
		this.containerPriority = copy.containerPriority;
		this.extractionOrder = copy.extractionOrder;
		this.id = copy.id;
		this.items = new ArrayList<>(copy.items);
		this.index = copy.index;
		for (BoxItem boxItem : items) {
			boxItem.setGroup(this);
		}
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

	public void setItems(List<BoxItem> items) {
		this.items = items;
	}

	public int size() {
		return items.size();
	}

	public BoxItem get(int i) {
		return items.get(i);
	}

	public boolean decrement(int index) {
		BoxItem boxItem = items.get(index);
		if (!boxItem.decrement()) {
			items.remove(index);
		}

		return !items.isEmpty();
	}

	public boolean decrement(int index, int count) {
		BoxItem boxItem = items.get(index);
		if (!boxItem.decrement(count)) {
			items.remove(index);
		}

		return !items.isEmpty();
	}

	public int getBoxCount() {
		int count = 0;
		for (BoxItem boxItem : items) {
			count += boxItem.getCount();
		}
		return count;
	}

	public boolean isEmpty() {
		for (BoxItem boxItem : items) {
			if (!boxItem.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			BoxItem boxItem = items.get(j);

			if (boxItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}

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

	public BoxItem remove(int index) {
		return items.remove(index);
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void reset() {
		this.items.clear();
		this.items.addAll(resetItems);
		for (BoxItem boxItem : resetItems) {
			boxItem.reset();
		}
	}

	public void mark() {
		if (resetItems == null) {
			resetItems = new ArrayList<>(items);
		} else {
			resetItems.clear();
			resetItems.addAll(items);
		}
		for (BoxItem boxItem : items) {
			boxItem.mark();
		}
	}

	@Override
	public String toString() {
		return id;
	}
}