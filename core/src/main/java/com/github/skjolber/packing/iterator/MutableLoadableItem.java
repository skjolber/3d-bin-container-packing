package com.github.skjolber.packing.iterator;

public class MutableLoadableItem extends IndexedStackableItem {

	public final IndexedStackableItem source; 

	public MutableLoadableItem(IndexedStackableItem loadableItem) {
		super(loadableItem.getStackable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
