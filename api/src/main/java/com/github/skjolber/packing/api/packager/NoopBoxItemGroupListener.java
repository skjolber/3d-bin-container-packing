package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

public class NoopBoxItemGroupListener implements BoxItemGroupListener {

	private static final NoopBoxItemGroupListener INSTANCE = new NoopBoxItemGroupListener(); 

	public static NoopBoxItemGroupListener getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void accepted(BoxItemGroup group) {
	}

}
