package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

public class NoopBoxItemListener implements BoxItemListener {

	private static final NoopBoxItemListener INSTANCE = new NoopBoxItemListener(); 

	public static BoxItemListener getInstance() {
		return INSTANCE;
	}

	@Override
	public void accepted(BoxItem boxItem) {
	}

}
