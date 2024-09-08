package com.github.skjolber.packing.api.packager;

import java.util.List;

/**
 * 
 * Items which belong together, for example different parts of a single product or order.
 * 
 */

public class LoadableItemGroup {

	private String id;
	private List<LoadableItem> items;

	public LoadableItemGroup(String id, List<LoadableItem> items) {
		super();
		this.id = id;
		this.items = items;
	}

	public String getId() {
		return id;
	}

	public List<LoadableItem> getItems() {
		return items;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setItems(List<LoadableItem> items) {
		this.items = items;
	}

	public int size() {
		return items.size();
	}
	
	public int loadableItemsCount() {
		int count = 0;
		for (LoadableItem loadableItem : items) {
			count += loadableItem.getCount();
		}
		return count;
	}

	public boolean isEmpty() {
		for (LoadableItem loadableItem : items) {
			if(!loadableItem.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeEmpty() {
		for (int j = 0; j < items.size(); j++) {
			LoadableItem loadableItem = items.get(j);
			
			if(loadableItem.isEmpty()) {
				items.remove(j);
				j--;
			}
		}
	}
}