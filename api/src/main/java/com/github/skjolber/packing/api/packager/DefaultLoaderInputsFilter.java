package com.github.skjolber.packing.api.packager;

/**
 * All items can be loaded and no dependencies across box items exist.
 * 
 */

public class DefaultLoaderInputsFilter implements LoaderInputsFilter {

	protected final LoaderInputs loaderInputs;

	public DefaultLoaderInputsFilter(LoaderInputs loadInputs) {
		this.loaderInputs = loadInputs;
	}

	@Override
	public void loaded(int index) {
		// do nothing
	}

}