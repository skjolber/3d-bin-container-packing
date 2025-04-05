package com.github.skjolber.packing.api.packager.inputs;

import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * Top-level packager inputs for packaging into one or more containers
 * 
 */

public interface PackagerInput {

	int getContainerItemInputSize();
	ContainerItemInput getContainerItemInput(int index);
	
	boolean removeContainerItem(ContainerItemInput input, int count);
	boolean removeBoxItem(BoxItem input, int count);

}
