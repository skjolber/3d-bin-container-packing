package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class BoxItemGroup {

	private String id;

	private List<BoxItem> items;

	public BoxItemGroup(String id, List<BoxItem> items) {
		super();
		this.id = id;
		this.items = items;
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
	
	
	public int stackableItemsCount() {
		int count = 0;
		for (BoxItem loadableItem : items) {
			count += loadableItem.getCount();
		}
		return count;
	}

	public boolean isEmpty() {
		for (BoxItem loadableItem : items) {
			if(!loadableItem.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			BoxItem loadableItem = items.get(j);
			
			if(loadableItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}
	
	public BoxItemGroup clone() {
		List<BoxItem> items = new ArrayList<>();

		for (BoxItem stackableItem : this.items) {
			items.add(stackableItem.clone());
		}
		
		return new BoxItemGroup(id, items);
	}
	
}