package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultFilteredBoxItems<T extends BoxItem> implements FilteredBoxItems<T> {

	private List<T> values;
	
	public DefaultFilteredBoxItems(List<T> values) {
		super();
		this.values = values;
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public T get(int index) {
		return values.get(index);
	}

	@Override
	public T decrement(int index, int count) {
		T boxItem = values.get(index);
		if(!boxItem.decrement(count)) {
			values.remove(index);
		}
		return boxItem;
	}
	
	@Override
	public T remove(int index) {
		return values.remove(index);
	}

	public void setValues(List<T> values) {
		this.values = values;
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	@Override
	public void clearEmpty() {
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				values.remove(i);
				i--;
			}
		}
	}

}
