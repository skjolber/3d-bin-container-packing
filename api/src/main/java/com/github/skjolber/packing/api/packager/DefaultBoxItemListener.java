package com.github.skjolber.packing.api.packager;

/**
 * All items can be loaded and no dependencies across box items exist.
 * 
 */

public class DefaultBoxItemListener implements BoxItemListener {

	protected final FilteredBoxItems input;

	public DefaultBoxItemListener(FilteredBoxItems input) {
		this.input = input;
	}

	@Override
	public void packed(int index) {
		// do nothing
	}

}