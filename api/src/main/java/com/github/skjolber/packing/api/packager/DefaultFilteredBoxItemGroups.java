package com.github.skjolber.packing.api.packager;

import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultFilteredBoxItemGroups implements FilteredBoxItemGroups {

	private List<BoxItemGroup> values;
	
	public DefaultFilteredBoxItemGroups(List<BoxItemGroup> values) {
		super();
		this.values = values;
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public BoxItemGroup get(int index) {
		return values.get(index);
	}

	@Override
	public BoxItemGroup remove(int index) {
		return values.remove(index);
	}

	public boolean isEmpty() {
		return values.isEmpty();
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

	@Override
	public Iterator<BoxItemGroup> iterator() {
		return values.listIterator();
	}

}
