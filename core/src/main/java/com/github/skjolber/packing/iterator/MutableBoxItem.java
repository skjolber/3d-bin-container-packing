package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.BoxItem;

public class MutableBoxItem extends BoxItem {

	private static final long serialVersionUID = 1L;
	
	public final BoxItem source; 

	public MutableBoxItem(BoxItem loadableItem) {
		super(loadableItem.getStackable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
