package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class IndexedStackableItemGroup {

	private String id;
	private List<IndexedStackableItem> items;

	public IndexedStackableItemGroup(String id, List<IndexedStackableItem> items) {
		super();
		this.id = id;
		this.items = items;
	}

	public String getId() {
		return id;
	}

	public List<IndexedStackableItem> getItems() {
		return items;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setItems(List<IndexedStackableItem> items) {
		this.items = items;
	}

	public int size() {
		return items.size();
	}
	
	public IndexedStackableItemGroup clone() {
		List<IndexedStackableItem> items = new ArrayList<>();

		for (IndexedStackableItem stackableItem : this.items) {
			items.add(stackableItem.clone());
		}
		
		return new IndexedStackableItemGroup(id, items);
	}

	public IndexedStackableItem get(int k) {
		return items.get(k);
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

}