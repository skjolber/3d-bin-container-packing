package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class IndexedStackableItem extends BoxItem {

	private static final long serialVersionUID = 1L;
	
	protected int index;
	
	public IndexedStackableItem(Stackable stackable, int count, int index) {
		super(stackable, count);
		this.index = index;
	}

	public boolean isEmpty() {
		return count == 0;
	}
	
	public int getIndex() {
		return index;
	}
	
	@Override
	public IndexedStackableItem clone() {
		return new IndexedStackableItem(box, count, index);
	}

}
