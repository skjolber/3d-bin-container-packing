package com.github.skjolber.packing.api.packager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * A box item group within a packaging operation: the box items of the group which remain to be packed (see
 * {@link RemainingBoxItem}). Packing changes these, not the {@link BoxItemGroup}, which is the input.
 */
public class RemainingBoxItemGroup implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final BoxItemGroup boxItemGroup;
	protected List<RemainingBoxItem> items;
	protected int index = -1;
	protected List<RemainingBoxItem> resetItems;

	public RemainingBoxItemGroup(BoxItemGroup boxItemGroup, List<RemainingBoxItem> items) {
		this(boxItemGroup, items, -1);
	}

	public RemainingBoxItemGroup(BoxItemGroup boxItemGroup, List<RemainingBoxItem> items, int index) {
		this.boxItemGroup = boxItemGroup;
		this.items = items;
		this.index = index;
		for (RemainingBoxItem boxItem : items) {
			boxItem.setGroup(this);
		}
	}

	/**
	 * @return the box item group (the input)
	 */
	public BoxItemGroup getBoxItemGroup() {
		return boxItemGroup;
	}

	public String getId() {
		return boxItemGroup.getId();
	}

	public int getContainerPriority() {
		return boxItemGroup.getContainerPriority();
	}

	public int getExtractionOrder() {
		return boxItemGroup.getExtractionOrder();
	}

	public List<RemainingBoxItem> getItems() {
		return items;
	}

	public int size() {
		return items.size();
	}

	public RemainingBoxItem get(int i) {
		return items.get(i);
	}

	public boolean decrement(int index) {
		RemainingBoxItem boxItem = items.get(index);
		if (!boxItem.decrement()) {
			items.remove(index);
		}
		return !items.isEmpty();
	}

	public boolean decrement(int index, int count) {
		RemainingBoxItem boxItem = items.get(index);
		if (!boxItem.decrement(count)) {
			items.remove(index);
		}
		return !items.isEmpty();
	}

	/**
	 * @return the number of boxes which remain
	 */
	public int getBoxCount() {
		int count = 0;
		for (RemainingBoxItem boxItem : items) {
			count += boxItem.getCount();
		}
		return count;
	}

	public boolean isEmpty() {
		for (RemainingBoxItem boxItem : items) {
			if (!boxItem.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			RemainingBoxItem boxItem = items.get(j);
			if (boxItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}

	/**
	 * @return a copy with copies of the remaining box items
	 */
	public RemainingBoxItemGroup copy() {
		List<RemainingBoxItem> items = new ArrayList<>(this.items.size());
		for (RemainingBoxItem boxItem : this.items) {
			items.add(boxItem.copy());
		}
		return new RemainingBoxItemGroup(boxItemGroup, items, index);
	}

	/**
	 * @return the volume of the boxes which remain
	 */
	public long getVolume() {
		long volume = 0;
		for (RemainingBoxItem boxItem : items) {
			volume += boxItem.getVolume();
		}
		return volume;
	}

	/**
	 * @return the weight of the boxes which remain
	 */
	public long getWeight() {
		long weight = 0;
		for (RemainingBoxItem boxItem : items) {
			weight += boxItem.getWeight();
		}
		return weight;
	}

	public RemainingBoxItem remove(int index) {
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
		for (RemainingBoxItem boxItem : resetItems) {
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
		for (RemainingBoxItem boxItem : items) {
			boxItem.mark();
		}
	}

	@Override
	public String toString() {
		return getId();
	}
}
