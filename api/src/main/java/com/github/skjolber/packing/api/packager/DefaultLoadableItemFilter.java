package com.github.skjolber.packing.api.packager;

public class DefaultLoadableItemFilter implements LoadableItemFilter {

	protected final LoadableItems loadableItems;

	public DefaultLoadableItemFilter(LoadableItems loadableItems) {
		this.loadableItems = loadableItems;
	}

	@Override
	public void loaded(int index) {
		LoadableItem loadableItem = loadableItems.get(index);
		loadableItem.decrement();
		if(loadableItem.isEmpty()) {
			loadableItems.remove(index);
		}
	}

}