package com.github.skjolber.packing.api.packager;
import java.util.List;

public class DefaultLoadableItemFilter implements LoadableItemFilter {

	protected final List<LoadableItem> loadableItems;

	public DefaultLoadableItemFilter(List<LoadableItem> loadableItems) {
		this.loadableItems = loadableItems;
	}

	@Override
	public List<LoadableItem> getLoadableItems() {
		return loadableItems;
	}

	@Override
	public boolean loaded(int index) {
		LoadableItem loadableItem = loadableItems.get(index);
		loadableItem.decrement();
		if(loadableItem.isEmpty()) {
			loadableItems.remove(index);
		}
		return false;
	}

}