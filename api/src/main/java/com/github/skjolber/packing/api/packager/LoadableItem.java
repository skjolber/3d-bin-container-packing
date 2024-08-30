package com.github.skjolber.packing.api.packager;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class LoadableItem {

	protected Loadable loadable;
	protected int count;
	
	public LoadableItem(Loadable loadable, int count) {
		this.loadable = loadable;
		this.count = count;
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
}
