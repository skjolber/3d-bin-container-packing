package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultFilteredBoxItemGroups implements FilteredBoxItemGroups {

	private List<BoxItemGroup> values;
	
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

}
