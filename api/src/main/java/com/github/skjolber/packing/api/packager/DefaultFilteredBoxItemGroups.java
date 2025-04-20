package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultFilteredBoxItemGroups<T extends BoxItemGroup> implements FilteredBoxItemGroups<T> {

	private List<T> values;
	
	public DefaultFilteredBoxItemGroups(List<T> values) {
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
	public T remove(int index) {
		return values.remove(index);
	}

	public boolean isEmpty() {
		return !values.isEmpty();
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
