package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultFilteredBoxItems implements FilteredBoxItems {

	private final List<BoxItem> values;
	
	public DefaultFilteredBoxItems(List<BoxItem> values) {
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
	public void remove(int index, int count) {
		BoxItem boxItem = values.get(index);
		if(!boxItem.decrement(count)) {
			values.remove(index);
		}
	}
	
 
}
