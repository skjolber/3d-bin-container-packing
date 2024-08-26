package com.github.skjolber.packing.api.packager;
import java.util.List;

public class DefaultLoadableItemScope implements LoadableItemScope {

	protected final List<LoadableItem> loadableItems;

	public DefaultLoadableItemScope(List<LoadableItem> loadableItems) {
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