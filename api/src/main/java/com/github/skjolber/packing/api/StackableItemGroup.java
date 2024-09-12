package com.github.skjolber.packing.api;

import java.util.List;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class StackableItemGroup {

	private String id;

	private List<StackableItem> items;

	public StackableItemGroup(String id, List<StackableItem> items) {
		super();
		this.id = id;
		this.items = items;
	}

	public String getId() {
		return id;
	}

	public List<StackableItem> getItems() {
		return items;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setItems(List<StackableItem> items) {
		this.items = items;
	}

	public int size() {
		return items.size();
	}

	public StackableItem get(int i) {
		return items.get(i);
	}
	
	public int loadableItemsCount() {
		int count = 0;
		for (StackableItem loadableItem : items) {
			count += loadableItem.getCount();
		}
		return count;
	}

	public boolean isEmpty() {
		for (StackableItem loadableItem : items) {
			if(!loadableItem.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			StackableItem loadableItem = items.get(j);
			
			if(loadableItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}
}