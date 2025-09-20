package com.github.skjolber.packing.api.packager;

public class DefaultManifestControls implements ManifestControls {

	protected BoxItemSource filteredBoxItems;
	
	public DefaultManifestControls(BoxItemSource filteredBoxItems) {
		this.filteredBoxItems = filteredBoxItems;
	}

}
