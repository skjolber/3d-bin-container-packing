package com.github.skjolber.packing.api.packager;

public class NoopBoxItemGroupListener implements BoxItemGroupListener {

	private static final NoopBoxItemGroupListener INSTANCE = new NoopBoxItemGroupListener(); 

	public static NoopBoxItemGroupListener getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void packedBoxItemGroup(int index) {
	}

}
