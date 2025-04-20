package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class BoxItemGroup<T extends BoxItem> {

	protected String id;

	protected List<T> items;
	
	protected int index = -1;
	
	public BoxItemGroup(String id, List<T> items) {
		super();
		this.id = id;
		this.items = items;
	}

	public String getId() {
		return id;
	}

	public List<T> getItems() {
		return items;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public int size() {
		return items.size();
	}

	public BoxItem get(int i) {
		return items.get(i);
	}
	
	public boolean decrement(int index) {
		T boxItem = items.get(index);
		if(!boxItem.decrement()) {
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
		for (T boxItem : items) {
			if(!boxItem.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			T boxItem = items.get(j);
			
			if(boxItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}
	
	public BoxItemGroup<T> clone() {
		List<T> items = new ArrayList<>();

		for (T boxItem : this.items) {
			items.add((T) boxItem.clone());
		}
		
		return new BoxItemGroup<>(id, items);
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

	public void remove(int index) {
		items.remove(index);
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

}