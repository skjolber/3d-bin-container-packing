package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.packager.FilteredBoxItems;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class BoxItemGroup {

	protected String id;

	protected List<BoxItem> items;
	
	protected int index = -1;

	protected List<BoxItem> resetItems;

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

	public BoxItemGroup(BoxItemGroup clone) {
		this.id = clone.id;
		this.items = new ArrayList<>(clone.items);
		this.index = clone.index;
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
		if(!boxItem.decrement()) {
			items.remove(index);
		}
		
		return !items.isEmpty();
	}
	
	public boolean decrement(int index, int count) {
		BoxItem boxItem = items.get(index);
		if(!boxItem.decrement(count)) {
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
			if(!boxItem.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			BoxItem boxItem = items.get(j);
			
			if(boxItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}
	
	public BoxItemGroup clone() {
		List<BoxItem> items = new ArrayList<>();

		for (BoxItem boxItem : this.items) {
			items.add(boxItem.clone());
		}
		
		return new BoxItemGroup(id, items);
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
		if(resetItems == null) {
			resetItems = new ArrayList<>(items);
		} else {
			resetItems.clear();
			resetItems.addAll(items);
		}
		for (BoxItem boxItem : items) {
			boxItem.mark();
		}
	}
	
}