package com.github.skjolber.packing.api.packager;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class LoadableItem {

	protected Loadable loadable;
	protected int count;
	protected int index;
	
	public LoadableItem(Loadable loadable, int count, int index) {
		this.loadable = loadable;
		this.count = count;
		this.index = index;
	}
	
	public Loadable getLoadable() {
		return loadable;
	}
	
	public void setLoadable(Loadable loadable) {
		this.loadable = loadable;
	}
	
	public int getCount() {
		return count;
	}
	
	public void decrement() {
		count--;
	}

	public boolean isEmpty() {
		return count == 0;
	}
	
	public int getIndex() {
		return index;
	}

	public void decrement(int value) {
		this.count = this.count - value;
	}
}
