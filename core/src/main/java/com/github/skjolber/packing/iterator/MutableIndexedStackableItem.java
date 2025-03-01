package com.github.skjolber.packing.iterator;

public class MutableIndexedStackableItem extends IndexedStackableItem {

	public final IndexedStackableItem source; 

	public MutableIndexedStackableItem(IndexedStackableItem loadableItem) {
		super(loadableItem.getStackable(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.source = loadableItem;
	}
	
	public void reset() {
		this.count = source.getCount();
	}

	
}
