package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.packager.BoundedStackableItem;

public class MutableLoadableItem extends BoundedStackableItem {

	public final BoundedStackableItem source; 

	public MutableLoadableItem(BoundedStackableItem loadableItem) {
		super(loadableItem.getLoadable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
