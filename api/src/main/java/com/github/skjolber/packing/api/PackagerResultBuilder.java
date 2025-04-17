package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class PackagerResultBuilder<B extends PackagerResultBuilder<B>> extends AbstractPackagerResultBuilder<B> {

	protected List<BoxItem> items = new ArrayList<>();

	public B withBoxItems(BoxItem... items) {
		List<BoxItem> list = new ArrayList<>(items.length);
		for (BoxItem item : items) {
			list.add(item);
		}
		return withBoxItems(list);
	}

	public B withBoxItems(List<BoxItem> items) {
		this.items = items;
		return (B)this;
	}

}
