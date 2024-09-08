package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.packager.LoadableItem;

public class MutableLoadableItem extends LoadableItem {

	public final LoadableItem source; 

	public MutableLoadableItem(LoadableItem loadableItem) {
		super(loadableItem.getLoadable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
