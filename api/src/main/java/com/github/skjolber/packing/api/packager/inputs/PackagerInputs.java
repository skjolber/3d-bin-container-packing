package com.github.skjolber.packing.api.packager.inputs;

/**
 * 
 * Top-level packager inputs for packaging into one or more containers
 * 
 */

public interface PackagerInputs {

	int ContainerItemInputSize();
	ContainerItemInput getContainerItemInput(int index);
	
	boolean removeContainerItem(ContainerItemInput input, int count);
	boolean removeStackableItem(StackableItemInput input, int count);

}
