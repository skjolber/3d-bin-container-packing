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
}