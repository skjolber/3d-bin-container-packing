package com.github.skjolber.packing.api.packager;

public class NoopBoxItemListener implements BoxItemListener {

	private static final NoopBoxItemListener INSTANCE = new NoopBoxItemListener(); 

	public static BoxItemListener getInstance() {
		return INSTANCE;
	}

	@Override
	public void packedBoxItem(int index) {
	}

}
