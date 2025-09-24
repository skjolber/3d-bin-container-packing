package com.github.skjolber.packing.api.packager.control.manifest;

import com.github.skjolber.packing.api.packager.BoxItemSource;

public class DefaultManifestControls implements ManifestControls {

	protected BoxItemSource filteredBoxItems;
	
	public DefaultManifestControls(BoxItemSource filteredBoxItems) {
		this.filteredBoxItems = filteredBoxItems;
	}

}
