package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.BoxItem;

public class MutableIndexedStackableItem extends BoxItem {

	public final BoxItem source; 

	public MutableIndexedStackableItem(BoxItem loadableItem) {
		super(loadableItem.getStackable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
