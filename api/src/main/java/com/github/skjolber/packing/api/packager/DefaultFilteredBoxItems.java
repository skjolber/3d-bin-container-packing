package com.github.skjolber.packing.api.packager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultFilteredBoxItems implements FilteredBoxItems {

	protected List<BoxItem> values;
	
	public DefaultFilteredBoxItems(List<BoxItem> values) {
		super();
		this.values = new ArrayList<>(values);
	}
	
	public DefaultFilteredBoxItems() {
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
	public boolean decrement(int index, int count) {
		BoxItem boxItem = values.get(index);
		if(!boxItem.decrement(count)) {
			values.remove(index);
		}
		return !values.isEmpty();
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
				remove(i);
				i--;
			}
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return values.listIterator();
	}

	@Override
	public FilteredBoxItemGroups getGroups() {
		return null;
	}

}
