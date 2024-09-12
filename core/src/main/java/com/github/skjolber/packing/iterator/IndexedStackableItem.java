package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.packager.BoundedStackable;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class IndexedStackableItem extends StackableItem {

	protected int index;
	
	public IndexedStackableItem(Stackable loadable, int count, int index) {
		super(loadable, count);
		this.index = index;
	}

	public boolean isEmpty() {
		return count == 0;
	}
	
	public int getIndex() {
		return index;
	}

}
