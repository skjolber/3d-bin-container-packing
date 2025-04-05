package com.github.skjolber.packing.api.packager;

/**
 * All items can be loaded and no dependencies across box items exist.
 * 
 */

public class DefaultPackInputFilter implements PackInputFilter {

	protected final PackInput scope;

	public DefaultPackInputFilter(PackInput scope) {
		this.scope = scope;
	}

	@Override
	public void loaded(int index) {
		// do nothing
	}

}