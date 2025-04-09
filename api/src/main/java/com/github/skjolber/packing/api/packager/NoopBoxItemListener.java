package com.github.skjolber.packing.api.packager;

/**
 * All items can be loaded and no dependencies across box items exist.
 * 
 */

public class NoopBoxItemListener implements BoxItemListener {

	protected final FilteredBoxItems input;

	public NoopBoxItemListener(FilteredBoxItems input) {
		this.input = input;
	}

	@Override
	public void packedBoxItem(int index) {
		// do nothing
	}

}