package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultFilteredBoxItems implements FilteredBoxItems {

	private List<BoxItem> values;
	
	public DefaultFilteredBoxItems(List<BoxItem> values) {
		super();
		this.values = values;
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public BoxItem get(int index) {
		return values.get(index);
	}

	@Override
	public BoxItem decrement(int index, int count) {
		BoxItem boxItem = values.get(index);
		if(!boxItem.decrement(count)) {
			values.remove(index);
		}
		return boxItem;
	}
	
	@Override
	public BoxItem remove(int index) {
		return values.remove(index);
	}

	public void setValues(List<BoxItem> values) {
		this.values = values;
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	@Override
	public void removeEmpty() {
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				values.remove(i);
				i--;
			}
		}
	}

}
